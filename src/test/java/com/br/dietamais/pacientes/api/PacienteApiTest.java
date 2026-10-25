package com.br.dietamais.pacientes.api;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.TesteIsolamento;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class PacienteApiTest extends TesteIsolamento {
	private static final String CHAVE_TESTE=chaveEfemera();
	@org.springframework.test.context.DynamicPropertySource
	static void propriedades(org.springframework.test.context.DynamicPropertyRegistry registro){registro.add("IDEMPOTENCIA_CHAVE",()->CHAVE_TESTE);}
	private static String chaveEfemera() {
		byte[] chave=new byte[32];new java.security.SecureRandom().nextBytes(chave);return Base64.getEncoder().encodeToString(chave);
	}
	@Test void cadastroConsultaEEdicaoVersionamFichaEAuditamSemDadosClinicos() throws Exception {
		var nutri=cadastrarNutri();
		var primeira=criar(nutri.token(),Map.of("nome","  Paciente fictício  ","email","PACIENTE@EXAMPLE.INVALID","objetivo","Objetivo sensível fictício"));
		assertThat(primeira.statusCode()).as(primeira.body()).isEqualTo(201);
		var inicial=json(primeira);String id=inicial.path("id").asString();
		assertThat(UUID.fromString(id).version()).isEqualTo(7);
		assertThat(inicial.path("nome").asString()).isEqualTo("Paciente fictício");
		assertThat(inicial.path("status").asString()).isEqualTo("CONVIDADO");
		assertThat(inicial.path("adesao7d").isNull()).isTrue();
		assertThat(primeira.headers().firstValue("Location").orElseThrow()).endsWith("/pacientes/"+id);
		assertThat(primeira.headers().firstValue("ETag").orElseThrow()).isEqualTo("\"0\"");
		var consulta=requisitar("GET","/pacientes/"+id,nutri.token(),null);
		assertThat(consulta.statusCode()).isEqualTo(200);
		var editada=editar(nutri.token(),id,"\"0\"",Map.of("nome","Novo nome fictício"));
		assertThat(editada.statusCode()).as(editada.body()).isEqualTo(200);
		assertThat(editada.headers().firstValue("ETag").orElseThrow()).isEqualTo("\"1\"");
		assertThat(json(editada).path("email").isNull()).isTrue();assertThat(json(editada).path("objetivo").isNull()).isTrue();
		assertThat(jdbc.queryForObject("select nome from paciente where id=?",String.class,UUID.fromString(id))).isEqualTo("Paciente fictício");
		assertThat(jdbc.queryForObject("select seq from ficha_paciente where paciente_id=?",Long.class,UUID.fromString(id))).isPositive();
		var logs=jdbc.queryForList("select * from log_acesso where usuario_id=?",nutri.usuario());
		assertThat(logs).hasSize(3);assertThat(logs.toString()).doesNotContain("Objetivo sensível fictício","paciente@example.invalid",nutri.token());
	}
	@Test void outraNutriNaoConsultaNemEditaEListaSoOsProprios() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();String id=id(criar(a.token(),Map.of("nome","Fictício A")));
		erro(requisitar("GET","/pacientes/"+id,b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(editar(b.token(),id,"\"0\"",Map.of("nome","Forjado")),404,"RECURSO_NAO_ENCONTRADO");
		assertThat(json(requisitar("GET","/pacientes",b.token(),null)).path("total").asLong()).isZero();
		assertThat(jdbc.queryForObject("select count(*) from log_acesso where usuario_id=?",Integer.class,b.usuario())).isZero();
	}
	@Test void conflitoDeVersaoDevolveAtualSemSobrescreverEIfMatchEObrigatorio() throws Exception {
		var a=cadastrarNutri();String id=id(criar(a.token(),Map.of("nome","Original fictício")));
		erro(requisitar("PATCH","/pacientes/"+id,a.token(),Map.of("nome","Ignorado")),400,"FORMATO_INVALIDO");
		erro(editar(a.token(),id,"W/\"0\"",Map.of("nome","Ignorado")),400,"FORMATO_INVALIDO");
		assertThat(editar(a.token(),id,"\"0\"",Map.of("nome","Primeira fictícia")).statusCode()).isEqualTo(200);
		var conflito=editar(a.token(),id,"\"0\"",Map.of("nome","Segunda fictícia"));erro(conflito,409,"CONFLITO_VERSAO");
		assertThat(json(conflito).path("atual").path("nome").asString()).isEqualTo("Primeira fictícia");
		assertThat(json(conflito).path("atual").path("versao").asLong()).isEqualTo(1);
	}
	@Test void limiteContaConvitesEAtivosELiberaVagaEncerrada() throws Exception {
		var a=cadastrarNutri();var ids=new ArrayList<String>();
		for(int i=0;i<5;i++)ids.add(id(criar(a.token(),Map.of("nome","Fictício "+i))));
		jdbc.update("update vinculo set status='ATIVO' where paciente_id=?",UUID.fromString(ids.getFirst()));
		erro(criar(a.token(),Map.of("nome","Sexto fictício")),422,"LIMITE_PACIENTES_ATINGIDO");
		jdbc.update("update vinculo set status='ENCERRADO' where paciente_id=?",UUID.fromString(ids.getFirst()));
		assertThat(criar(a.token(),Map.of("nome","Vaga liberada fictícia")).statusCode()).isEqualTo(201);
		erro(editar(a.token(),ids.getFirst(),"\"0\"",Map.of("nome","Tentativa")),403,"ACESSO_NEGADO");
		assertThat(requisitar("GET","/pacientes/"+ids.getFirst(),a.token(),null).statusCode()).isEqualTo(200);
	}
	@Test void concorrenciaNaUltimaVagaEEdicaoDaMesmaVersaoTemApenasUmVencedor() throws Exception {
		var a=cadastrarNutri();String id=null;
		for(int i=0;i<4;i++)id=id(criar(a.token(),Map.of("nome","Fictício "+i)));
		try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
			var tarefas=List.<Callable<Integer>>of(()->criar(a.token(),Map.of("nome","Último A")).statusCode(),()->criar(a.token(),Map.of("nome","Último B")).statusCode());
			var resultados=executor.invokeAll(tarefas);assertThat(List.of(resultados.get(0).get(),resultados.get(1).get())).containsExactlyInAnyOrder(201,422);
			String paciente=id;
			var edicoes=List.<Callable<Integer>>of(()->editar(a.token(),paciente,"\"0\"",Map.of("nome","Edição A")).statusCode(),()->editar(a.token(),paciente,"\"0\"",Map.of("nome","Edição B")).statusCode());
			var atualizacoes=executor.invokeAll(edicoes);assertThat(List.of(atualizacoes.get(0).get(),atualizacoes.get(1).get())).containsExactlyInAnyOrder(200,409);
		}
	}
	@Test void buscaFiltroPaginacaoEOrdenacaoSaoIsoladosEValidados() throws Exception {
		var a=cadastrarNutri();id(criar(a.token(),Map.of("nome","Alice fictícia")));String b=id(criar(a.token(),Map.of("nome","Bruna fictícia")));
		jdbc.update("update vinculo set status='ATIVO' where paciente_id=?",UUID.fromString(b));
		var busca=json(requisitar("GET","/pacientes?busca=BRUNA&status=ATIVO",a.token(),null));assertThat(busca.path("total").asLong()).isEqualTo(1);
		var pagina=json(requisitar("GET","/pacientes?tamanho=1&pagina=1&ordem=nome,asc",a.token(),null));
		assertThat(pagina.path("total").asLong()).isEqualTo(2);assertThat(pagina.path("itens").size()).isEqualTo(1);
		assertThat(pagina.path("itens").get(0).path("nome").asString()).isEqualTo("Bruna fictícia");
		assertThat(json(requisitar("GET","/pacientes?busca=%25",a.token(),null)).path("total").asLong()).isZero();
		for(String query:List.of("pagina=-1","tamanho=101","ordem=senha,asc","status=INVALIDO"))erro(requisitar("GET","/pacientes?"+query,a.token(),null),400,"FORMATO_INVALIDO");
	}
	@Test void duplicidadeValidacaoEIdempotenciaNaoCriamFichasExtras() throws Exception {
		var a=cadastrarNutri();var corpo=Map.of("nome","Fictício","email","unico@example.invalid");String chave=UUID.randomUUID().toString();
		var primeira=requisitar("POST","/pacientes",a.token(),corpo,Map.of("Idempotency-Key",chave));assertThat(primeira.statusCode()).as(primeira.body()).isEqualTo(201);
		var repetida=requisitar("POST","/pacientes",a.token(),corpo,Map.of("Idempotency-Key",chave));assertThat(repetida.body()).isEqualTo(primeira.body());
		erro(criar(a.token(),corpo),409,"VINCULO_JA_EXISTE");
		erro(criar(a.token(),Map.of("nome","   ")),400,"OBRIGATORIO");
		erro(criar(a.token(),Map.of("nome","Fictício","email","invalido")),400,"FORMATO_INVALIDO");
		erro(criar(a.token(),Map.of("nome","Fictício","id",UUID.randomUUID())),400,"FORMATO_INVALIDO");
		erro(criar(a.token(),Map.of("nome","Fictício","estaturaReferencia",9)),400,"FORMATO_INVALIDO");
		assertThat(jdbc.queryForObject("select count(*) from ficha_paciente where nutricionista_id=?",Integer.class,a.nutri())).isEqualTo(1);
		jdbc.update("update assinatura set status='EXPIRADA' where organizacao_id=?",a.organizacao());
		erro(requisitar("POST","/pacientes",a.token(),corpo,Map.of("Idempotency-Key",chave)),422,"ASSINATURA_INATIVA");
		assertThat(requisitar("GET","/pacientes",a.token(),null).statusCode()).isEqualTo(200);
	}
	@Test void limitePertenceAOrganizacaoMesmoComDuasNutris() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();
		jdbc.update("update nutricionista set organizacao_id=? where id=?",a.organizacao(),b.nutri());
		for(int i=0;i<4;i++)id(criar(a.token(),Map.of("nome","Fictício "+i)));
		assertThat(criar(b.token(),Map.of("nome","Quinta ficha fictícia")).statusCode()).isEqualTo(201);
		erro(criar(a.token(),Map.of("nome","Sexta ficha fictícia")),422,"LIMITE_PACIENTES_ATINGIDO");
		assertThat(json(requisitar("GET","/pacientes",b.token(),null)).path("total").asLong()).isEqualTo(1);
	}
	@Test void mesmoPacienteGlobalTemFichaIndependenteParaCadaNutriEIdClienteNaoVinculaProntuarioAlheio() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();
		String paciente=id(criar(a.token(),Map.of("nome","Nome A fictício","nutricionistaId",b.nutri())));
		UUID vinculo=jdbc.queryForObject("insert into vinculo(nutricionista_id,paciente_id) values(?,?) returning id",UUID.class,b.nutri(),UUID.fromString(paciente));
		jdbc.update("insert into ficha_paciente(id,vinculo_id,nutricionista_id,paciente_id,nome) values(?,?,?,?,?)",vinculo,vinculo,b.nutri(),UUID.fromString(paciente),"Nome B fictício");
		assertThat(editar(a.token(),paciente,"\"0\"",Map.of("nome","Alteração A fictícia")).statusCode()).isEqualTo(200);
		assertThat(json(requisitar("GET","/pacientes/"+paciente,b.token(),null)).path("nome").asString()).isEqualTo("Nome B fictício");
		erro(criar(b.token(),Map.of("id",paciente,"nome","Tentativa fictícia")),409,"VINCULO_JA_EXISTE");
	}
	private HttpResponse<String> criar(String token,Map<String,?> corpo) throws Exception{return requisitar("POST","/pacientes",token,corpo);}
	private HttpResponse<String> editar(String token,String id,String versao,Map<String,?> corpo) throws Exception{return requisitar("PATCH","/pacientes/"+id,token,corpo,Map.of("If-Match",versao));}
	private String id(HttpResponse<String> resposta){assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(201);return json(resposta).path("id").asString();}
}
