package com.br.dietamais.dietas;

import com.br.dietamais.comum.TesteIsolamento;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PublicarDietasVinculoApiTest extends TesteIsolamento {
	@Test void publicarEDuplicarNaoReabremHistoricoQuandoExisteNovoVinculoAtivo()throws Exception{
		var conta=cadastrarNutri();var cadastro=requisitar("POST","/pacientes",conta.token(),Map.of("nome","Paciente fictício","email",UUID.randomUUID()+"@example.invalid"));assertThat(cadastro.statusCode()).isEqualTo(201);
		var paciente=json(cadastro);String pid=paciente.path("id").asString();UUID antigo=UUID.fromString(paciente.path("vinculoId").asString());
		String food=json(requisitar("GET","/alimentos",conta.token(),null)).path("itens").get(0).path("id").asString();
		var corpo=Map.of("titulo","Dieta fictícia","refeicoes",List.of(Map.of("nome","Refeição fictícia","horario","12:00:00","ordem",0,"itens",List.of(Map.of("alimentoId",food,"porcoes",1)))));
		var criacao=requisitar("POST","/pacientes/"+pid+"/dietas",conta.token(),corpo);assertThat(criacao.statusCode()).as(criacao.body()).isEqualTo(201);var dieta=json(criacao);String id=dieta.path("id").asString();String etag="\""+dieta.path("versao").asLong()+"\"";
		assertThat(requisitar("POST","/pacientes/"+pid+"/encerrar-vinculo",conta.token(),null).statusCode()).isEqualTo(200);
		UUID novo=jdbc.queryForObject("select uuidv7()",UUID.class);UUID pacienteId=UUID.fromString(pid);
		jdbc.update("insert into vinculo(id,nutricionista_id,paciente_id,status) values(?,?,?,'ATIVO')",novo,conta.nutri(),pacienteId);
		jdbc.update("insert into ficha_paciente(id,vinculo_id,nutricionista_id,paciente_id,nome) values(?,?,?,?,?)",novo,novo,conta.nutri(),pacienteId,"Ficha atual fictícia");
		assertThat(jdbc.queryForObject("select status from vinculo where id=?",String.class,antigo)).isEqualTo("ENCERRADO");assertThat(json(requisitar("GET","/pacientes/"+pid,conta.token(),null)).path("vinculoId").asString()).isEqualTo(novo.toString());
		erro(requisitar("POST","/dietas/"+id+"/publicar",conta.token(),null,Map.of("If-Match",etag)),403,"ACESSO_NEGADO");erro(requisitar("POST","/dietas/"+id+"/duplicar",conta.token(),null),403,"ACESSO_NEGADO");
		assertThat(jdbc.queryForObject("select count(*) from dieta_snapshot where dieta_id=?",Integer.class,UUID.fromString(id))).isZero();assertThat(jdbc.queryForObject("select count(*) from dieta where vinculo_id=?",Integer.class,antigo)).isEqualTo(1);
		var historico=requisitar("GET","/dietas/"+id,conta.token(),null);assertThat(historico.statusCode()).isEqualTo(200);assertThat(json(historico)).isEqualTo(dieta);
		var atual=requisitar("POST","/pacientes/"+pid+"/dietas",conta.token(),corpo);assertThat(atual.statusCode()).as(atual.body()).isEqualTo(201);var nova=json(atual);String atualId=nova.path("id").asString();
		var publicado=requisitar("POST","/dietas/"+atualId+"/publicar",conta.token(),null,Map.of("If-Match","\""+nova.path("versao").asLong()+"\""));assertThat(publicado.statusCode()).as(publicado.body()).isEqualTo(200);assertThat(json(publicado).path("vinculoId").asString()).isEqualTo(novo.toString());
		var copia=requisitar("POST","/dietas/"+atualId+"/duplicar",conta.token(),null);assertThat(copia.statusCode()).as(copia.body()).isEqualTo(201);assertThat(json(copia).path("vinculoId").asString()).isEqualTo(novo.toString());
	}
}
