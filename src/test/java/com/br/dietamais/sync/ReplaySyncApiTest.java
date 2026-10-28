package com.br.dietamais.sync;

import com.br.dietamais.dietas.TesteComPaciente;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import static org.assertj.core.api.Assertions.*;

class ReplaySyncApiTest extends TesteComPaciente {
	@Test void mesmaContaComDoisPerfisNaoRecebeResultadoClinicoDoOutroPerfil()throws Exception{
		var a=cadastrarNutri();var paciente=pacienteVinculado(a);jdbc.update("update paciente set usuario_id=? where id=?",a.usuario(),UUID.fromString(paciente.pacienteId()));String tokenPaciente=entrar(a.email(),"PACIENTE");
		String avaliacao=id();var dados=new HashMap<String,Object>(Map.of("pacienteId",paciente.fichaId(),"dataHora",Instant.now().minusSeconds(3600).toString(),"pesoKg",70,"estaturaM",1.70));
		assertThat(enviar(a.token(),mutacao("avaliacoes","CRIAR",avaliacao,null,dados)).path("status").asString()).isEqualTo("APLICADA");dados.remove("pacienteId");var m=mutacao("avaliacoes","ATUALIZAR",avaliacao,0L,dados);
		var conflito=enviar(a.token(),m);assertThat(conflito.path("status").asString()).isEqualTo("CONFLITO");assertThat(conflito.has("atual")).isTrue();reuso(enviar(tokenPaciente,m));
		var agua=mutacao("agua","CRIAR",id(),null,Map.of("ml",250,"registradoEm",Instant.now().toString()));assertThat(enviar(tokenPaciente,agua).path("status").asString()).isEqualTo("APLICADA");reuso(enviar(a.token(),agua));
	}
	@Test void replayLegitimoCorpoAlteradoELegadoNaoGeramNovasEscritas()throws Exception{
		var a=cadastrarNutri();var m=mutacao("pacientes","CRIAR",id(),null,Map.of("nome","Ficha fictícia"));var primeiro=enviar(a.token(),m);assertThat(primeiro.path("status").asString()).isEqualTo("APLICADA");assertThat(enviar(a.token(),m)).isEqualTo(primeiro);
		var alterada=new HashMap<>(m);alterada.put("dados",Map.of("nome","Outra ficha fictícia"));reuso(enviar(a.token(),alterada));assertThat(jdbc.queryForObject("select nome from ficha_paciente where paciente_id=? and nutricionista_id=?",String.class,UUID.fromString((String)m.get("id")),a.nutri())).isEqualTo("Ficha fictícia");
		var legado=mutacao("pacientes","CRIAR",id(),null,Map.of("nome","Legado fictício"));UUID chave=UUID.fromString((String)legado.get("idMutacao"));jdbc.update("insert into mutacao_sync(id_mutacao,usuario_id,resultado) values(?,?,?::jsonb)",chave,a.usuario(),mapper.writeValueAsString(Map.of("idMutacao",chave,"status","APLICADA","versao",0)));
		reuso(enviar(a.token(),legado));assertThat(jdbc.queryForObject("select count(*) from paciente where id=?",Integer.class,UUID.fromString((String)legado.get("id")))).isZero();assertThat(jdbc.queryForObject("select fingerprint from mutacao_sync where id_mutacao=?",String.class,chave)).isNull();
	}
	@Test void concorrenciaMesmaChaveTemUmaAplicacaoEReplayDeterministico()throws Exception{
		var a=cadastrarNutri();var m=mutacao("pacientes","CRIAR",id(),null,Map.of("nome","Concorrência fictícia"));
		try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
			var inicio=new CountDownLatch(1);Callable<JsonNode> acao=()->{inicio.await();return enviar(a.token(),m);};var um=executor.submit(acao);var dois=executor.submit(acao);inicio.countDown();var primeiro=um.get(30,TimeUnit.SECONDS);assertThat(primeiro.path("status").asString()).isEqualTo("APLICADA");assertThat(dois.get(30,TimeUnit.SECONDS)).isEqualTo(primeiro);
		}
		assertThat(jdbc.queryForObject("select count(*) from ficha_paciente where paciente_id=? and nutricionista_id=?",Integer.class,UUID.fromString((String)m.get("id")),a.nutri())).isEqualTo(1);
	}
	@Test void concorrenciaMesmaChaveComCorposDistintosNaoConfirmaDuasMutacoes()throws Exception{
		var a=cadastrarNutri();var m=mutacao("pacientes","CRIAR",id(),null,Map.of("nome","Primeira fictícia"));var outro=new HashMap<>(m);outro.put("dados",Map.of("nome","Segunda fictícia"));
		try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
			var inicio=new CountDownLatch(1);var um=executor.submit(()->{inicio.await();return enviar(a.token(),m);});var dois=executor.submit(()->{inicio.await();return enviar(a.token(),outro);});inicio.countDown();var resultados=List.of(um.get(30,TimeUnit.SECONDS),dois.get(30,TimeUnit.SECONDS));assertThat(resultados.stream().map(r->r.path("status").asString())).containsExactlyInAnyOrder("APLICADA","REJEITADA");resultados.stream().filter(r->r.path("status").asString().equals("REJEITADA")).forEach(this::reuso);
		}
		assertThat(jdbc.queryForObject("select count(*) from ficha_paciente where paciente_id=? and nutricionista_id=?",Integer.class,UUID.fromString((String)m.get("id")),a.nutri())).isEqualTo(1);
	}
	String id(){return jdbc.queryForObject("select uuidv7()",String.class);}
	Map<String,Object> mutacao(String entidade,String operacao,String id,Long versao,Object dados){var m=new HashMap<String,Object>();m.put("idMutacao",UUID.randomUUID().toString());m.put("entidade",entidade);m.put("operacao",operacao);m.put("id",id);m.put("versaoBase",versao);m.put("dados",dados);return m;}
	JsonNode enviar(String token,Map<String,Object> m)throws Exception{var resposta=requisitar("POST","/sync",token,Map.of("mutacoes",List.of(m)));assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(200);return json(resposta).path("resultados").get(0);}
	void reuso(JsonNode resultado){assertThat(resultado.path("status").asString()).isEqualTo("REJEITADA");assertThat(resultado.path("codigo").asString()).isEqualTo("CHAVE_IDEMPOTENCIA_REUTILIZADA");assertThat(resultado.has("atual")).isFalse();assertThat(resultado.has("versao")).isFalse();}
}
