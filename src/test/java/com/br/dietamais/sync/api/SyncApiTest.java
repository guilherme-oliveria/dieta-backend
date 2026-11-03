package com.br.dietamais.sync.api;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.TesteIsolamento;
import com.br.dietamais.comum.api.Perfil;
import com.br.dietamais.contas.infra.TokensJwt;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

class SyncApiTest extends TesteIsolamento {
	@Autowired TokensJwt tokens;
	@Autowired Clock relogio;
	@Autowired PlatformTransactionManager transacoes;
	@Test void cargaPaginadaEIncrementalSaoIsoladasERevisaoNaoEVersaoParaIfMatch() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var pa=criar(a.token(),"OBS_A_FICTICIA");var pb=criar(b.token(),"OBS_B_FICTICIA");
		String cursor=null;var revisoes=new ArrayList<Long>();var pacientes=new ArrayList<String>();int paginas=0;
		do {
			var pagina=sync(a.token(),cursor,1);paginas++;int quantidade=0;
			for(String colecao:List.of("pacientes","vinculos","nutris","grupos","alimentos","listas","dietas","snapshots","dietaEmUso"))for(var item:pagina.path("mudancas").path(colecao)) {
				quantidade++;revisoes.add(item.path("versao").asLong());if(colecao.equals("pacientes"))pacientes.add(item.path("id").asString());
			}
			assertThat(quantidade).isLessThanOrEqualTo(1);cursor=pagina.path("cursor").asString();if(!pagina.path("temMais").asBoolean())break;
		}while(paginas<3000);
		assertThat(paginas).isLessThan(3000);assertThat(pacientes).containsExactly(pa.path("id").asString());assertThat(revisoes).isSorted().doesNotHaveDuplicates();
		var vazio=sync(a.token(),cursor,1000);assertThat(vazio.path("mudancas").path("pacientes").isEmpty()).isTrue();
		var privado=sync(a.token(),null,1000);assertThat(privado.toString()).contains("OBS_A_FICTICIA").doesNotContain("OBS_B_FICTICIA");
		var item=item(privado,"pacientes",pa.path("id").asString());assertThat(item.path("versao").asLong()).isGreaterThan(item.path("dados").path("versao").asLong());
		var errado=requisitar("PATCH","/pacientes/"+pa.path("id").asString(),a.token(),Map.of("nome","Fictício"),Map.of("If-Match","\""+item.path("versao").asLong()+"\""));erro(errado,409,"CONFLITO_VERSAO");
		assertThat(sync(b.token(),null,1000).toString()).contains("OBS_B_FICTICIA").doesNotContain("OBS_A_FICTICIA");
		for(String consulta:List.of("cursor=-1","cursor=9999999999999999999","limite=0","limite=1001"))erro(requisitar("GET","/sync?"+consulta,a.token(),null),400,"FORMATO_INVALIDO");
	}
	@Test void pacienteNaoRecebeFichaPrivadaEEncerramentoMantemHistorico() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var p=criar(b.token(),"OBS_PRIVADA_FICTICIA");String pid=p.path("id").asString();String paciente=vincularConta(p,a.usuario());
		var antes=sync(paciente,null,1000);assertThat(antes.path("mudancas").path("pacientes").isEmpty()).isTrue();assertThat(antes.toString()).doesNotContain("OBS_PRIVADA_FICTICIA");
		assertThat(item(antes,"vinculos",p.path("vinculoId").asString()).path("dados").path("status").asString()).isEqualTo("ATIVO");
		assertThat(requisitar("POST","/pacientes/"+pid+"/encerrar-vinculo",b.token(),null).statusCode()).isEqualTo(200);
		var depois=sync(paciente,antes.path("cursor").asString(),1000);var vinculo=item(depois,"vinculos",p.path("vinculoId").asString());
		assertThat(vinculo.path("excluido").asBoolean()).isFalse();assertThat(vinculo.path("dados").path("status").asString()).isEqualTo("ENCERRADO");
		assertThat(item(depois,"nutris",b.nutri().toString()).path("excluido").asBoolean()).isFalse();
		assertThat(item(sync(b.token(),null,1000),"pacientes",pid).path("dados").path("status").asString()).isEqualTo("ENCERRADO");
	}
	@Test void perdaRealEnviaTombstonesEReaparecimentoEnviaDados() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var p=criar(b.token(),"OBS_PRIVADA_FICTICIA");String paciente=vincularConta(p,a.usuario());
		var inicial=sync(paciente,null,1000);String antes=inicial.path("cursor").asString();UUID vinculo=UUID.fromString(p.path("vinculoId").asString());
		jdbc.update("update vinculo set excluido_em=?,atualizado_em=?,versao=versao+1 where id=?",agora(),agora(),vinculo);
		var excluido=sync(paciente,antes,1000);var tombstone=item(excluido,"vinculos",vinculo.toString());assertThat(tombstone.path("excluido").asBoolean()).isTrue();assertThat(tombstone.path("dados").isNull()).isTrue();
		assertThat(item(excluido,"nutris",b.nutri().toString()).path("dados").isNull()).isTrue();
		assertThat(item(sync(b.token(),null,1000),"pacientes",p.path("id").asString()).path("excluido").asBoolean()).isTrue();
		erro(requisitar("GET","/pacientes/"+p.path("id").asString(),b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		jdbc.update("update vinculo set excluido_em=null,atualizado_em=?,versao=versao+1 where id=?",agora(),vinculo);
		var restaurado=sync(paciente,excluido.path("cursor").asString(),1000);var reaparecido=item(restaurado,"vinculos",vinculo.toString());
		assertThat(reaparecido.path("excluido").asBoolean()).isFalse();assertThat(reaparecido.path("versao").asLong()).isGreaterThan(tombstone.path("versao").asLong());
	}
	@Test void trocaDePlaceholderENovoVinculoNaoPerdemRevisaoMesmoComVersaoClinicaMenor() throws Exception {
		var a=cadastrarNutri();var antigo=criar(a.token(),"OBS_ANTIGA_FICTICIA");UUID pid=UUID.fromString(antigo.path("id").asString());
		jdbc.update("update ficha_paciente set versao=9,seq=nextval('seq_sync'),atualizado_em=? where vinculo_id=?",agora(),UUID.fromString(antigo.path("vinculoId").asString()));
		assertThat(requisitar("POST","/pacientes/"+pid+"/encerrar-vinculo",a.token(),null).statusCode()).isEqualTo(200);
		var encerrado=item(sync(a.token(),null,1000),"pacientes",pid.toString());var novo=criar(a.token(),"OBS_NOVA_FICTICIA");var antes=sync(a.token(),null,1000);
		new TransactionTemplate(transacoes).executeWithoutResult(status->{
			jdbc.update("update vinculo set paciente_origem_id=paciente_id,paciente_id=?,status='ATIVO',consentimento_em=?,versao=versao+1,atualizado_em=? where id=?",pid,agora(),agora(),UUID.fromString(novo.path("vinculoId").asString()));
			jdbc.update("update ficha_paciente set paciente_id=?,versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where vinculo_id=?",pid,agora(),UUID.fromString(novo.path("vinculoId").asString()));
		});
		var depois=sync(a.token(),antes.path("cursor").asString(),1000);var atual=item(depois,"pacientes",pid.toString());
		assertThat(atual.path("versao").asLong()).isGreaterThan(encerrado.path("versao").asLong());assertThat(atual.path("dados").path("versao").asLong()).isLessThan(encerrado.path("dados").path("versao").asLong());
		assertThat(atual.path("dados").path("vinculoId").asString()).isEqualTo(novo.path("vinculoId").asString());
		assertThat(item(depois,"pacientes",novo.path("id").asString()).path("excluido").asBoolean()).isTrue();
	}
	@Test void cursorNaoUltrapassaTransacaoAbertaNemPerdeCommitAtrasado() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var pa=criar(a.token(),"OBS_A_FICTICIA");var pb=criar(b.token(),"OBS_B_FICTICIA");String cursor=sync(a.token(),null,1000).path("cursor").asString();
		var iniciou=new CountDownLatch(1);var liberar=new CountDownLatch(1);String tag="be08-"+UUID.randomUUID();
		try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
			var primeira=executor.submit(()->{new TransactionTemplate(transacoes).executeWithoutResult(status->{atualizarObservacao(pa,"OBS_A_ATRASADA_FICTICIA");iniciou.countDown();aguardar(liberar);});return null;});
			aguardar(iniciou);
			var segunda=executor.submit(()->{new TransactionTemplate(transacoes).executeWithoutResult(status->{jdbc.queryForObject("select set_config('application_name',?,true)",String.class,tag);atualizarObservacao(pb,"OBS_B_FICTICIA_ATUALIZADA");});return null;});
			try {
				long fim=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);boolean bloqueada=false;
				while(System.nanoTime()<fim){bloqueada=jdbc.queryForObject("select exists(select 1 from pg_stat_activity where application_name=? and wait_event_type='Lock')",Boolean.class,tag);if(bloqueada)break;Thread.sleep(10);}
				assertThat(bloqueada).isTrue();var durante=sync(a.token(),cursor,1000);assertThat(durante.path("cursor").asString()).isEqualTo(cursor);assertThat(durante.path("mudancas").path("pacientes").isEmpty()).isTrue();
			}finally{liberar.countDown();}
			primeira.get(10,TimeUnit.SECONDS);segunda.get(10,TimeUnit.SECONDS);
		}
		assertThat(sync(a.token(),cursor,1000).toString()).contains("OBS_A_ATRASADA_FICTICIA").doesNotContain("OBS_B_FICTICIA_ATUALIZADA");assertThat(sync(b.token(),cursor,1000).toString()).contains("OBS_B_FICTICIA_ATUALIZADA");
	}
	@Test void nomeECrnDaNutriAtualizamFonteSemExporConta() throws Exception {
		var a=cadastrarNutri();String cursor=sync(a.token(),null,1000).path("cursor").asString();
		jdbc.update("update usuario set nome='Nutri fictícia atualizada' where id=?",a.usuario());jdbc.update("update nutricionista set crn='CRN-1 998877' where id=?",a.nutri());
		var resposta=sync(a.token(),cursor,1000);var nutri=item(resposta,"nutris",a.nutri().toString());assertThat(nutri.path("dados").path("nome").asString()).isEqualTo("Nutri fictícia atualizada");assertThat(nutri.path("dados").path("crn").asString()).isEqualTo("CRN-1 998877");assertThat(resposta.toString()).doesNotContain(a.email(),"senha_hash");
	}
	JsonNode criar(String token,String observacoes) throws Exception {var resposta=requisitar("POST","/pacientes",token,Map.of("nome","Paciente fictício","observacoes",observacoes));assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(201);return json(resposta);}
	JsonNode sync(String token,String cursor,int limite) throws Exception {var resposta=requisitar("GET","/sync?limite="+limite+(cursor==null?"":"&cursor="+cursor),token,null);assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(200);return json(resposta);}
	JsonNode item(JsonNode resposta,String colecao,String id){for(var item:resposta.path("mudancas").path(colecao))if(item.path("id").asString().equals(id))return item;throw new AssertionError("Mudança esperada ausente na coleção "+colecao);}
	Timestamp agora(){return Timestamp.from(relogio.instant());}
	String vincularConta(JsonNode ficha,UUID usuario) {var pid=UUID.fromString(ficha.path("id").asString());jdbc.update("update paciente set usuario_id=? where id=?",usuario,pid);jdbc.update("update vinculo set status='ATIVO',consentimento_em=?,versao=versao+1,atualizado_em=? where id=?",agora(),agora(),UUID.fromString(ficha.path("vinculoId").asString()));return tokens.emitir(usuario,Perfil.PACIENTE,null,pid).valor();}
	void atualizarObservacao(JsonNode ficha,String observacao){jdbc.update("update ficha_paciente set observacoes=?,versao=versao+1,seq=nextval('seq_sync'),atualizado_em=? where vinculo_id=?",observacao,agora(),UUID.fromString(ficha.path("vinculoId").asString()));}
	void aguardar(CountDownLatch sinal){try{if(!sinal.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Prazo de teste expirou");}catch(InterruptedException erro){Thread.currentThread().interrupt();throw new IllegalStateException("Teste interrompido",erro);}}
}
