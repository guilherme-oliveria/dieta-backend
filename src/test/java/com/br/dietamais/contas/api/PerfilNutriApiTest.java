package com.br.dietamais.contas.api;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.TesteIsolamento;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class PerfilNutriApiTest extends TesteIsolamento {
	@Test void perfilVersionadoEProprioNaoAlteraContaGlobalNemOutraNutri() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();
		var inicial=requisitar("GET","/eu/nutri?nutricionistaId="+b.nutri(),a.token(),null);
		assertThat(inicial.statusCode()).isEqualTo(200);assertThat(json(inicial).path("id").asString()).isEqualTo(a.nutri().toString());
		String etag=inicial.headers().firstValue("ETag").orElseThrow();assertThat(etag).isEqualTo("\"0\"");
		jdbc.update("update nutricionista set crn_verificado=true where id=?",a.nutri());
		var atualizado=requisitar("PATCH","/eu/nutri",a.token(),dados(" Exibição fictícia ","CRN-2 99999","  "," Texto fictício "),Map.of("If-Match",etag));
		assertThat(atualizado.statusCode()).isEqualTo(200);var json=json(atualizado);
		assertThat(json.path("nome").asString()).isEqualTo("Exibição fictícia");assertThat(json.path("telefone").isNull()).isTrue();assertThat(json.path("orientacaoPadrao").asString()).isEqualTo("Texto fictício");
		assertThat(json.path("versao").asLong()).isEqualTo(1);assertThat(atualizado.headers().firstValue("ETag").orElseThrow()).isEqualTo("\"1\"");
		assertThat(atualizado.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
		assertThat(jdbc.queryForObject("select nome from usuario where id=?",String.class,a.usuario())).isEqualTo("Nutri fictícia");
		assertThat(jdbc.queryForObject("select crn_verificado from nutricionista where id=?",Boolean.class,a.nutri())).isFalse();
		assertThat(json(requisitar("GET","/eu/nutri",b.token(),null)).path("nome").asString()).isEqualTo("Nutri fictícia");
		var conflito=requisitar("PATCH","/eu/nutri",a.token(),dados("Outro fictício","CRN-1 00000",null,null),Map.of("If-Match",etag));
		erro(conflito,409,"CONFLITO_VERSAO");assertThat(json(conflito).path("atual").path("id").asString()).isEqualTo(a.nutri().toString());
	}
	@Test void assinaturaPerfilEFormatoContinuamValidados() throws Exception {
		var a=cadastrarNutri();var corpo=dados("Fictício","CRN-1 00000",null,null);
		erro(requisitar("PATCH","/eu/nutri",a.token(),corpo),400,"FORMATO_INVALIDO");
		erro(requisitar("PATCH","/eu/nutri",a.token(),corpo,Map.of("If-Match","W/\"0\"")),400,"FORMATO_INVALIDO");
		erro(requisitar("PATCH","/eu/nutri",a.token(),dados("Fictício","CRN-invalido",null,null),Map.of("If-Match","\"0\"")),400,"FORMATO_INVALIDO");
		jdbc.update("update assinatura set status='CANCELADA' where organizacao_id=?",a.organizacao());
		erro(requisitar("PATCH","/eu/nutri",a.token(),corpo,Map.of("If-Match","\"0\"")),422,"ASSINATURA_INATIVA");
		assertThat(requisitar("GET","/eu/nutri",a.token(),null).statusCode()).isEqualTo(200);
		jdbc.update("insert into paciente(usuario_id,nome) values(?,'Paciente fictício')",a.usuario());String paciente=entrar(a.email(),"PACIENTE");
		erro(requisitar("GET","/eu/nutri",paciente,null),403,"PERFIL_INVALIDO");
		erro(requisitar("PATCH","/eu/nutri",paciente,corpo,Map.of("If-Match","\"0\"")),403,"PERFIL_INVALIDO");
	}
	@Test void duasEscritasComMesmaVersaoNaoSobrescrevemSilenciosamente() throws Exception {
		var a=cadastrarNutri();var largada=new CountDownLatch(1);
		try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
			var tarefas=new ArrayList<Future<Integer>>();
			for(String nome:List.of("Primeiro fictício","Segundo fictício"))tarefas.add(executor.submit(()->{largada.await();return requisitar("PATCH","/eu/nutri",a.token(),dados(nome,"CRN-1 00000",null,null),Map.of("If-Match","\"0\"")).statusCode();}));
			largada.countDown();assertThat(List.of(tarefas.get(0).get(15,TimeUnit.SECONDS),tarefas.get(1).get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
		}
		assertThat(json(requisitar("GET","/eu/nutri",a.token(),null)).path("versao").asLong()).isEqualTo(1);
	}
	@Test void nomePublicoAtualizadoChegaAoSyncSemTelefoneOuOrientacao() throws Exception {
		var a=cadastrarNutri();var antes=json(requisitar("GET","/sync",a.token(),null)).path("cursor").asString();
		assertThat(requisitar("PATCH","/eu/nutri",a.token(),dados("Exibição no sync","CRN-1 00000","123","Texto privado fictício"),Map.of("If-Match","\"0\"")).statusCode()).isEqualTo(200);
		var resposta=requisitar("GET","/sync?cursor="+antes,a.token(),null);assertThat(resposta.statusCode()).isEqualTo(200);
		assertThat(json(resposta).path("mudancas").path("nutris").get(0).path("dados").path("nome").asString()).isEqualTo("Exibição no sync");
		assertThat(resposta.body()).doesNotContain("telefone","orientacaoPadrao","Texto privado fictício");
	}
	Map<String,Object> dados(String nome,String crn,String telefone,String orientacao){var dados=new HashMap<String,Object>();dados.put("nome",nome);dados.put("crn",crn);dados.put("telefone",telefone);dados.put("orientacaoPadrao",orientacao);return dados;}
}
