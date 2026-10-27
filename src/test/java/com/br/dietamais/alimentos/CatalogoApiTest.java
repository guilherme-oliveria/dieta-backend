package com.br.dietamais.alimentos;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.TesteIsolamento;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class CatalogoApiTest extends TesteIsolamento {
	@Test void cargaEConsultaPublicamCatalogoGlobalSemDadosPrivados() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();
		var resposta=requisitar("GET","/grupos-alimentos?nutricionistaId="+b.nutri(),a.token(),null);
		assertThat(resposta.statusCode()).isEqualTo(200);assertThat(resposta.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
		assertThat(json(resposta).path("itens").size()).isEqualTo(12);
		for(var grupo:json(resposta).path("itens"))assertThat(grupo.path("macrosEstimados").asBoolean()).isTrue();
		assertThat(json(requisitar("GET","/grupos-alimentos",b.token(),null))).isEqualTo(json(resposta));
		assertThat(resposta.body()).doesNotContain(a.email(),b.email(),"usuarioId","nutricionistaId","senha","paciente");
		assertThat(jdbc.queryForObject("select count(*) from alimento where ativo",Integer.class)).isEqualTo(482);
		assertThat(jdbc.queryForObject("select count(*) from fonte_macros_grupo where estimativa",Integer.class)).isEqualTo(12);
		assertThat(jdbc.queryForObject("select count(*) from grupo_alimento where carboidratos_por_porcao is null or proteinas_por_porcao is null or gorduras_por_porcao is null",Integer.class)).isZero();
		assertThat(requisitar("GET","/alimentos",a.token(),null).statusCode()).isEqualTo(501);
	}
	@Test void identidadePerfilEPlanoSaoConferidosSemDarEscritaNoCatalogo() throws Exception {
		erro(requisitar("GET","/grupos-alimentos",null,null),401,"NAO_AUTENTICADO");var a=cadastrarNutri();
		jdbc.update("update assinatura set status='CANCELADA' where organizacao_id=?",a.organizacao());
		assertThat(requisitar("GET","/grupos-alimentos",a.token(),null).statusCode()).isEqualTo(200);
		jdbc.update("insert into paciente(usuario_id,nome) values(?,'Paciente fictício')",a.usuario());
		erro(requisitar("GET","/grupos-alimentos",entrar(a.email(),"PACIENTE"),null),403,"PERFIL_INVALIDO");
		jdbc.update("update usuario set ativo=false where id=?",a.usuario());
		erro(requisitar("GET","/grupos-alimentos",a.token(),null),401,"NAO_AUTENTICADO");
	}
	@Test void valoresSaoDadosConfiguraveisDoBancoSemConstantesNaResposta() throws Exception {
		var a=cadastrarNutri();var linha=jdbc.queryForMap("select id,proteinas_por_porcao,versao from grupo_alimento where nome='Pão'");
		UUID id=(UUID)linha.get("id");BigDecimal anterior=(BigDecimal)linha.get("proteinas_por_porcao");long versao=((Number)linha.get("versao")).longValue();
		try{
			// Fixture administrativa: não existe API de escrita do catálogo para nutris nesta tarefa.
			jdbc.update("update grupo_alimento set proteinas_por_porcao=9.9,versao=versao+1 where id=?",id);
			var itens=json(requisitar("GET","/grupos-alimentos",a.token(),null)).path("itens");
			tools.jackson.databind.JsonNode encontrado=null;
			for(var grupo:itens)if(grupo.path("id").asString().equals(id.toString()))encontrado=grupo;
			assertThat(encontrado).isNotNull();assertThat(encontrado.path("proteinasPorPorcao").decimalValue()).isEqualByComparingTo("9.9");
		}finally{jdbc.update("update grupo_alimento set proteinas_por_porcao=?,versao=? where id=?",anterior,versao,id);}
	}
}
