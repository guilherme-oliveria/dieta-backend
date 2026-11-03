package com.br.dietamais.contas.api;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.TestcontainersConfiguration;
import com.br.dietamais.notificacoes.aplicacao.ProcessarEmails;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
	"dieta.auth.login-limite-ip=10000","dieta.auth.recuperacao-limite-ip=10000"})
@Import({TestcontainersConfiguration.class,EmailSenhaApiTest.Configuracao.class})
class EmailSenhaApiTest {
	static final Instant AGORA=Instant.parse("2026-10-25T12:00:00Z");
	static final String SENHA="Frase fictícia original com 2026 luas";
	static final String NOVA="Frase fictícia nova com 2027 estrelas";
	@Autowired JdbcTemplate jdbc;
	@Autowired JsonMapper mapper;
	@Autowired PasswordEncoder encoder;
	@Autowired ProcessarEmails job;
	@Autowired TestcontainersConfiguration.TransporteEmailTeste transporte;
	@Autowired RelogioTeste relogio;
	@LocalServerPort int porta;
	final HttpClient cliente=HttpClient.newHttpClient();
	@AfterEach void limpar(){cliente.close();relogio.agora.set(AGORA);transporte.falhar=false;}
	@TestConfiguration(proxyBeanMethods=false)
	static class Configuracao { @Bean @Primary RelogioTeste relogioEmail(){return new RelogioTeste();} }
	static class RelogioTeste extends Clock {
		final AtomicReference<Instant> agora=new AtomicReference<>(AGORA);
		public ZoneId getZone(){return ZoneOffset.UTC;} public Clock withZone(ZoneId zona){return this;} public Instant instant(){return agora.get();}
	}
	@Test void cadastroEnfileiraCifradoEConfirmacaoConsomeUmaVezSemConfirmarOutroUsuario() throws Exception {
		String a=cadastrar();String b=cadastrar();
		var payloads=jdbc.queryForList("select payload_cifrado from notificacao_pendente where status='PENDENTE'",String.class);
		assertThat(payloads).allSatisfy(payload->assertThat(payload).startsWith("v1.").doesNotContain(a,b,"@").isNotBlank());
		String token=tokenEnviado(a,"Confirme");
		assertThat(payloads).allSatisfy(payload->assertThat(payload).doesNotContain(token));
		assertThat(jdbc.queryForObject("select hash_token from token_uso_unico where usuario_id=? and finalidade='CONFIRMAR_EMAIL'",String.class,id(a))).isNotEqualTo(token).hasSize(64);
		var resposta=post("confirmar-email",Map.of("token",token,"usuarioId",id(b)));
		assertThat(resposta.statusCode()).isEqualTo(204);assertThat(resposta.body()).isEmpty();
		assertThat(jdbc.queryForObject("select email_confirmado_em from usuario where email=?",java.sql.Timestamp.class,a).toInstant()).isEqualTo(AGORA);
		assertThat(jdbc.queryForObject("select email_confirmado_em is null from usuario where email=?",Boolean.class,b)).isTrue();
		erro(post("confirmar-email",Map.of("token",token)),401,"TOKEN_INVALIDO","RN-CON-05");
		assertThat(jdbc.queryForObject("select count(*) from notificacao_pendente where status='ENVIADA' and payload_cifrado is not null",Integer.class)).isZero();
	}
	@Test void confirmacaoVenceExatamenteEmVinteEQuatroHoras() throws Exception {
		String email=cadastrar();String token=tokenEnviado(email,"Confirme");
		relogio.agora.set(AGORA.plus(Duration.ofHours(24)));
		erro(post("confirmar-email",Map.of("token",token)),401,"TOKEN_INVALIDO","RN-CON-05");
	}
	@Test void recuperacaoRetornaMesmo202SemCorpoParaExistenteInativoEDesconhecido() throws Exception {
		String ativo=cadastrar();String inativo=cadastrar();jdbc.update("update usuario set ativo=false where email=?",inativo);
		for(String email:List.of(ativo,inativo,UUID.randomUUID()+"@example.invalid")) {
			var resposta=post("esqueci-senha",Map.of("email","  "+email.toUpperCase(Locale.ROOT)+"  "));
			assertThat(resposta.statusCode()).isEqualTo(202);assertThat(resposta.body()).isEmpty();
		}
		assertThat(jdbc.queryForObject("select count(*) from token_uso_unico where usuario_id=? and finalidade='REDEFINIR_SENHA'",Integer.class,id(ativo))).isEqualTo(1);
		assertThat(jdbc.queryForObject("select count(*) from token_uso_unico where usuario_id=? and finalidade='REDEFINIR_SENHA'",Integer.class,id(inativo))).isZero();
	}
	@Test void redefinirAplicaArgonERevogaTodosDispositivosSomenteDoDonoDoToken() throws Exception {
		String email=cadastrar();String outro=cadastrar();String refreshA=refresh(email);String refreshB=refresh(email);String refreshOutro=refresh(outro);
		assertThat(post("esqueci-senha",Map.of("email",email)).statusCode()).isEqualTo(202);
		String token=tokenEnviado(email,"Redefina");
		var resposta=post("redefinir-senha",Map.of("token",token,"novaSenha",NOVA,"usuarioId",id(outro)));
		assertThat(resposta.statusCode()).isEqualTo(204);assertThat(resposta.body()).isEmpty();
		assertThat(encoder.matches(NOVA,jdbc.queryForObject("select senha_hash from usuario where email=?",String.class,email))).isTrue();
		assertThat(encoder.matches(SENHA,jdbc.queryForObject("select senha_hash from usuario where email=?",String.class,outro))).isTrue();
		erro(post("refresh",Map.of("refreshToken",refreshA)),401,"TOKEN_INVALIDO",null);
		erro(post("refresh",Map.of("refreshToken",refreshB)),401,"TOKEN_INVALIDO",null);
		assertThat(post("refresh",Map.of("refreshToken",refreshOutro)).statusCode()).isEqualTo(200);
		erro(post("redefinir-senha",Map.of("token",token,"novaSenha",NOVA)),401,"TOKEN_INVALIDO","RN-CON-07");
	}
	@Test void finalidadeSenhaFracaEExpiracaoSaoValidadasSemConsumirIndevidamente() throws Exception {
		String email=cadastrar();String confirmacao=tokenEnviado(email,"Confirme");
		erro(post("redefinir-senha",Map.of("token",confirmacao,"novaSenha",NOVA)),401,"TOKEN_INVALIDO","RN-CON-07");
		post("esqueci-senha",Map.of("email",email));String reset=tokenEnviado(email,"Redefina");
		erro(post("confirmar-email",Map.of("token",reset)),401,"TOKEN_INVALIDO","RN-CON-05");
		erro(post("redefinir-senha",Map.of("token",reset,"novaSenha","password123")),400,"FORMATO_INVALIDO","RN-CON-02");
		assertThat(post("redefinir-senha",Map.of("token",reset,"novaSenha",NOVA)).statusCode()).isEqualTo(204);
		post("esqueci-senha",Map.of("email",email));String vencido=tokenEnviado(email,"Redefina");
		relogio.agora.set(AGORA.plus(Duration.ofHours(1)));
		erro(post("redefinir-senha",Map.of("token",vencido,"novaSenha",NOVA)),401,"TOKEN_INVALIDO","RN-CON-07");
	}
	@Test void novaSolicitacaoAnulaLinkAnteriorEConsumoConcorrenteTemUmSucesso() throws Exception {
		String email=cadastrar();post("esqueci-senha",Map.of("email",email));String antigo=tokenEnviado(email,"Redefina");
		post("esqueci-senha",Map.of("email",email));String novo=tokenEnviado(email,"Redefina");
		erro(post("redefinir-senha",Map.of("token",antigo,"novaSenha",NOVA)),401,"TOKEN_INVALIDO","RN-CON-07");
		try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
			var largada=new CountDownLatch(1);
			var a=executor.submit(()->{largada.await();return post("redefinir-senha",Map.of("token",novo,"novaSenha",NOVA));});
			var b=executor.submit(()->{largada.await();return post("redefinir-senha",Map.of("token",novo,"novaSenha",NOVA));});largada.countDown();
			assertThat(List.of(a.get().statusCode(),b.get().statusCode())).containsExactlyInAnyOrder(204,401);
		}
	}
	@Test void falhaDeOutboxRetorna202SemEnumerarERollbackPreservaTokenAnterior() throws Exception {
		String email=cadastrar();post("esqueci-senha",Map.of("email",email));String anterior=tokenEnviado(email,"Redefina");
		int antes=jdbc.queryForObject("select count(*) from token_uso_unico where usuario_id=?",Integer.class,id(email));
		jdbc.execute("alter table notificacao_pendente add constraint ck_email_falha_teste check(false) not valid");
		try {
			assertThat(post("esqueci-senha",Map.of("email",email)).statusCode()).isEqualTo(202);
			assertThat(post("esqueci-senha",Map.of("email",UUID.randomUUID()+"@example.invalid")).statusCode()).isEqualTo(202);
			assertThat(jdbc.queryForObject("select count(*) from token_uso_unico where usuario_id=?",Integer.class,id(email))).isEqualTo(antes);
		} finally {jdbc.execute("alter table notificacao_pendente drop constraint ck_email_falha_teste");}
		assertThat(post("redefinir-senha",Map.of("token",anterior,"novaSenha",NOVA)).statusCode()).isEqualTo(204);
	}
	@Test void falhaDeOutboxNoCadastroDesfazContaOrganizacaoEAssinatura() throws Exception {
		int antes=jdbc.queryForObject("select count(*) from organizacao",Integer.class);String email=UUID.randomUUID()+"@example.invalid";
		jdbc.execute("alter table notificacao_pendente add constraint ck_email_falha_cadastro check(false) not valid");
		try {
			assertThat(cadastro(email).statusCode()).isEqualTo(500);
			assertThat(jdbc.queryForObject("select count(*) from usuario where email=?",Integer.class,email)).isZero();
			assertThat(jdbc.queryForObject("select count(*) from organizacao",Integer.class)).isEqualTo(antes);
		} finally {jdbc.execute("alter table notificacao_pendente drop constraint ck_email_falha_cadastro");}
	}
	@Test void jobRepeteCincoVezesSemDuplicarEnvioEApagaPayloadDefinitivo() throws Exception {
		drenar();String email=cadastrar();UUID notificacao=jdbc.queryForObject("select id from notificacao_pendente where status='PENDENTE'",UUID.class);
		transporte.falhar=true;
		for(int tentativa=1;tentativa<=5;tentativa++) {
			assertThat(job.processarProxima()).isTrue();
			assertThat(jdbc.queryForObject("select tentativas from notificacao_pendente where id=?",Integer.class,notificacao)).isEqualTo(tentativa);
			relogio.agora.set(AGORA.plusSeconds(tentativa*600));
		}
		assertThat(jdbc.queryForObject("select status from notificacao_pendente where id=?",String.class,notificacao)).isEqualTo("FALHA");
		assertThat(jdbc.queryForObject("select payload_cifrado is null from notificacao_pendente where id=?",Boolean.class,notificacao)).isTrue();
		assertThat(transporte.mensagens.stream().filter(mensagem->mensagem.destinatario().equals(email))).isEmpty();
	}
	@Test void jobConcorrenteEnviaUmaVezEJobDescartaMensagemVencida() throws Exception {
		drenar();String email=cadastrar();
		try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
			var a=executor.submit(()->job.processarProxima());var b=executor.submit(()->job.processarProxima());a.get();b.get();
		}
		assertThat(transporte.mensagens.stream().filter(mensagem->mensagem.destinatario().equals(email))).hasSize(1);
		String vencido=cadastrar();relogio.agora.set(AGORA.plus(Duration.ofHours(24)));drenar();
		assertThat(transporte.mensagens.stream().filter(mensagem->mensagem.destinatario().equals(vencido))).isEmpty();
		assertThat(jdbc.queryForObject("select count(*) from notificacao_pendente where status='EXPIRADA' and payload_cifrado is not null",Integer.class)).isZero();
	}

	String cadastrar() throws Exception {String email=UUID.randomUUID()+"@example.invalid";assertThat(cadastro(email).statusCode()).isEqualTo(201);return email;}
	HttpResponse<String> cadastro(String email) throws Exception {return post("cadastro-nutri",Map.of("nome","Nutri fictícia","email",email,"senha",SENHA,"crn","CRN-1 8814","aceitouTermos",true,"versaoTermos","2026-10"));}
	UUID id(String email){return jdbc.queryForObject("select id from usuario where email=?",UUID.class,email);}
	String refresh(String email) throws Exception {
		var resposta=post("login",Map.of("email",email,"senha",SENHA,"dispositivo",Map.of("id",UUID.randomUUID(),"plataforma","ANDROID","nomeAparelho","Teste")));
		assertThat(resposta.statusCode()).isEqualTo(200);return mapper.readTree(resposta.body()).path("refreshToken").asString();
	}
	void drenar(){for(int i=0;i<100 && job.processarProxima();i++) { }}
	String tokenEnviado(String email,String assunto) {
		drenar();var mensagem=transporte.mensagens.stream().filter(item->item.destinatario().equals(email)&&item.assunto().startsWith(assunto)).toList().getLast();
		var regex=java.util.regex.Pattern.compile("token=([A-Za-z0-9_-]{43})").matcher(mensagem.html());assertThat(regex.find()).isTrue();return regex.group(1);
	}
	HttpResponse<String> post(String operacao,Object corpo) throws Exception {
		return cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:"+porta+"/api/v1/auth/"+operacao)).header("Content-Type","application/json")
				.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(corpo))).build(),HttpResponse.BodyHandlers.ofString());
	}
	void erro(HttpResponse<String> resposta,int status,String codigo,String regra) {
		assertThat(resposta.statusCode()).isEqualTo(status);JsonNode json=mapper.readTree(resposta.body());
		assertThat(json.path("codigo").asString()).isEqualTo(codigo);
		if(regra!=null)assertThat(json.path("regra").asString()).isEqualTo(regra);
		assertThat(resposta.body()).doesNotContain(SENHA,NOVA,"hash_token","senha_hash");
	}
}
