package com.br.dietamais.assinaturas;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.assinaturas.aplicacao.CicloAssinatura;
import com.br.dietamais.dietas.TesteComPaciente;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CicloAssinaturaTest extends TesteComPaciente {
	@Autowired CicloAssinatura ciclo;

	void fim(ContaTeste a,String status,String intervalo){
		jdbc.update("update assinatura set status=?,fim_periodo_em=now()+?::interval where organizacao_id=?",status,intervalo,a.organizacao());
	}
	String status(ContaTeste a){return jdbc.queryForObject("select status from assinatura where organizacao_id=?",String.class,a.organizacao());}
	int avisos(ContaTeste a) throws Exception {return json(requisitar("GET","/eu/avisos",a.token(),null)).path("itens").size();}

	@Test void testeExpiraUmaVezEAvisa() throws Exception {
		var a=cadastrarNutri();fim(a,"TESTE","-1 hour");
		ciclo.executar();
		assertThat(status(a)).isEqualTo("EXPIRADA");assertThat(avisos(a)).isEqualTo(1);
		ciclo.executar();assertThat(avisos(a)).isEqualTo(1);
		erro(requisitar("POST","/pacientes",a.token(),Map.of("nome","Fulano")),422,"ASSINATURA_INATIVA");
	}

	@Test void avisaQuandoOTesteEstaAcabando() throws Exception {
		var a=cadastrarNutri();fim(a,"TESTE","2 days");
		ciclo.executar();ciclo.executar();
		assertThat(status(a)).isEqualTo("TESTE");assertThat(avisos(a)).isEqualTo(1);
		var longe=cadastrarNutri();fim(longe,"TESTE","20 days");ciclo.executar();assertThat(avisos(longe)).isZero();
	}

	@Test void ativaSemRenovacaoVaiParaAtrasoDepoisDaToleranciaEBloqueiaDepoisDaCarencia() throws Exception {
		var a=cadastrarNutri();fim(a,"ATIVA","-2 days");
		ciclo.executar();assertThat(status(a)).isEqualTo("ATIVA"); // dentro da tolerância do webhook
		fim(a,"ATIVA","-4 days");ciclo.executar();
		assertThat(status(a)).isEqualTo("EM_ATRASO");assertThat(avisos(a)).isEqualTo(1);
		// ainda na carência: edita
		assertThat(requisitar("POST","/pacientes",a.token(),Map.of("nome","Fulano")).statusCode()).isEqualTo(201);
		fim(a,"EM_ATRASO","-8 days");ciclo.executar();ciclo.executar();
		assertThat(avisos(a)).isEqualTo(2);
		erro(requisitar("POST","/pacientes",a.token(),Map.of("nome","Beltrano")),422,"ASSINATURA_INATIVA");
		// o paciente continua vendo a dieta e os avisos (RN-ASS-05)
		var p=pacienteVinculado(cadastrarNutri());
		assertThat(requisitar("GET","/eu/avisos",p.token(),null).statusCode()).isEqualTo(200);
	}

	@Test void downgradeBloqueiaNovosConvitesSemEncerrarVinculos() throws Exception {
		var a=cadastrarNutri();
		pacienteVinculado(a);pacienteVinculado(a);
		jdbc.update("insert into plano(codigo,nome,preco_mensal,limite_pacientes) values('MINI_1','Mini',9.90,1)");
		jdbc.update("update assinatura set plano_id=(select id from plano where codigo='MINI_1'),status='ATIVA',fim_periodo_em=now()+interval '20 days' where organizacao_id=?",a.organizacao());
		erro(requisitar("POST","/pacientes",a.token(),Map.of("nome","Novo")),422,"LIMITE_PACIENTES_ATINGIDO");
		assertThat(jdbc.queryForObject("select count(*) from vinculo where nutricionista_id=? and status='ATIVO'",Integer.class,a.nutri())).isEqualTo(2);
		jdbc.update("update plano set ativo=false where codigo='MINI_1'");
	}
}
