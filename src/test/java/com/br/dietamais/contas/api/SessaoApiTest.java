package com.br.dietamais.contas.api;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.TestcontainersConfiguration;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
	"dieta.auth.login-limite-ip=10000", "WEB_ORIGEM=https://app.example.invalid" })
@Import({ TestcontainersConfiguration.class, SessaoApiTest.Configuracao.class })
class SessaoApiTest {
	private static final Instant AGORA = Instant.parse("2026-10-25T12:00:00Z");
	private static final String SENHA = "Frase fictícia de sessão com 2026 luas";
	@Autowired JdbcTemplate jdbc;
	@Autowired JsonMapper mapper;
	@Autowired JwtDecoder decoder;
	@Autowired RelogioTeste relogio;
	@LocalServerPort int porta;
	final HttpClient cliente = HttpClient.newHttpClient();
	@AfterEach void limparRelogio() { cliente.close(); relogio.agora.set(AGORA); }
	@TestConfiguration(proxyBeanMethods = false)
	static class Configuracao { @Bean @Primary RelogioTeste relogioSessao() { return new RelogioTeste(); } }
	static class RelogioTeste extends Clock {
		final AtomicReference<Instant> agora = new AtomicReference<>(AGORA);
		public ZoneId getZone() { return ZoneOffset.UTC; }
		public Clock withZone(ZoneId zona) { return this; }
		public Instant instant() { return agora.get(); }
	}

	@Test void loginEmiteJwtERefreshHashSemBasicOuCache() throws Exception {
		String email = cadastrar(); var dispositivo = UUID.randomUUID();
		var resposta = login(email.toUpperCase(Locale.ROOT), SENHA, dispositivo, null);
		assertThat(resposta.statusCode()).isEqualTo(200);
		assertThat(resposta.headers().firstValue("Cache-Control")).contains("no-store");
		var sessao = json(resposta); String access = texto(sessao,"accessToken"); String refresh = texto(sessao,"refreshToken");
		var jwt = decoder.decode(access);
		assertThat(jwt.getSubject()).isEqualTo(texto(sessao.path("usuario"),"id"));
		assertThat(jwt.getClaimAsString("perfil")).isEqualTo("NUTRICIONISTA");
		assertThat(jwt.getExpiresAt()).isEqualTo(AGORA.plusSeconds(900));
		assertThat(refresh).matches("[A-Za-z0-9_-]{43}");
		var linha = jdbc.queryForMap("select * from refresh_token where usuario_id=?", UUID.fromString(jwt.getSubject()));
		assertThat(linha.get("hash_token")).isNotEqualTo(refresh);
		assertThat(linha.get("dispositivo_id")).isEqualTo(dispositivo);
		assertThat(linha.get("nome_aparelho")).isEqualTo("Aparelho fictício");
		assertThat(jdbc.queryForObject("select expira_em from refresh_token where usuario_id=?", java.sql.Timestamp.class, UUID.fromString(jwt.getSubject())).toInstant()).isEqualTo(AGORA.plus(Duration.ofDays(30)));
		assertThat(getEu("Bearer " + access).statusCode()).isEqualTo(200);
		assertThat(getEu("Basic " + Base64.getEncoder().encodeToString("ficticio:SEM_VALIDADE".getBytes())).statusCode()).isEqualTo(401);
		assertThat(getEu("Bearer invalido.SEM_VALIDADE.assinatura").statusCode()).isEqualTo(401);
		relogio.agora.set(AGORA.plusSeconds(900));
		assertThat(getEu("Bearer " + access).statusCode()).isEqualTo(401);
		assertThat(renovar(refresh).statusCode()).isEqualTo(200);
	}
	@Test void cincoFalhasBloqueiamEmailsConhecidosEDesconhecidosSemVazarExistencia() throws Exception {
		String existente = cadastrar(); String inexistente = UUID.randomUUID()+"@example.invalid";
		for(String email: List.of(existente, inexistente)) {
			for(int i=0;i<4;i++) erro(login(email,"Senha fictícia incorreta",UUID.randomUUID(),null),401,"CREDENCIAIS_INVALIDAS");
			var bloqueado = login(email,"Senha fictícia incorreta",UUID.randomUUID(),null);
			erro(bloqueado,429,"LOGIN_BLOQUEADO");
			assertThat(bloqueado.headers().firstValue("Retry-After")).contains("900");
		}
		erro(login(existente,SENHA,UUID.randomUUID(),null),429,"LOGIN_BLOQUEADO");
		relogio.agora.set(AGORA.plusSeconds(901));
		assertThat(login(existente,SENHA,UUID.randomUUID(),null).statusCode()).isEqualTo(200);
	}
	@Test void rotacaoEReusoRevogamFamiliaMasPreservamOutroDispositivo() throws Exception {
		String email=cadastrar(); String antigo=token(login(email,SENHA,UUID.randomUUID(),null));
		String outro=token(login(email,SENHA,UUID.randomUUID(),null));
		String sucessor=token(renovar(antigo));
		assertThat(sucessor).isNotEqualTo(antigo);
		erro(renovar(antigo),401,"TOKEN_INVALIDO");
		erro(renovar(sucessor),401,"TOKEN_INVALIDO");
		assertThat(renovar(outro).statusCode()).isEqualTo(200);
	}
	@Test void logoutRevogaSoFamiliaDoDispositivoERepete204() throws Exception {
		String email=cadastrar(); String primeiro=token(login(email,SENHA,UUID.randomUUID(),null));
		String outro=token(login(email,SENHA,UUID.randomUUID(),null));
		assertThat(post("/auth/logout",Map.of("refreshToken",primeiro)).statusCode()).isEqualTo(204);
		assertThat(post("/auth/logout",Map.of("refreshToken",primeiro)).statusCode()).isEqualTo(204);
		erro(renovar(primeiro),401,"TOKEN_INVALIDO");
		assertThat(renovar(outro).statusCode()).isEqualTo(200);
	}
	@Test void refreshExpiradoNaoEmiteSucessor() throws Exception {
		String email=cadastrar(); String token=token(login(email,SENHA,UUID.randomUUID(),null));
		int antes=jdbc.queryForObject("select count(*) from refresh_token",Integer.class);
		relogio.agora.set(AGORA.plus(Duration.ofDays(30)));
		erro(renovar(token),401,"TOKEN_INVALIDO");
		assertThat(jdbc.queryForObject("select count(*) from refresh_token",Integer.class)).isEqualTo(antes);
	}
	@Test void concorrenciaNoMesmoRefreshTemUmSucessoERevogaSucessorPorReuso() throws Exception {
		String token=token(login(cadastrar(),SENHA,UUID.randomUUID(),null));
		try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
			var largada=new CountDownLatch(1);
			var a=executor.submit(()-> {largada.await();return renovar(token);});
			var b=executor.submit(()-> {largada.await();return renovar(token);}); largada.countDown();
			var ra=a.get();var rb=b.get();
			assertThat(List.of(ra.statusCode(),rb.statusCode())).containsExactlyInAnyOrder(200,401);
			erro(renovar(token(ra.statusCode()==200?ra:rb)),401,"TOKEN_INVALIDO");
		}
	}
	@Test void perfilVemDaIdentidadeEEscolhaNaoPermiteEscalarParaAdmin() throws Exception {
		String email=cadastrar(); UUID uid=jdbc.queryForObject("select id from usuario where email=?",UUID.class,email);
		UUID pid=jdbc.queryForObject("insert into paciente(id,usuario_id,nome) values(uuidv7(),?,?) returning id",UUID.class,uid,"Paciente fictício");
		var escolha=json(login(email,SENHA,UUID.randomUUID(),null));
		assertThat(escolha.path("accessToken").isNull()).isTrue();
		assertThat(escolha.path("refreshToken").isNull()).isTrue();
		assertThat(escolha.path("usuario").path("perfis").size()).isEqualTo(2);
		erro(login(email,SENHA,UUID.randomUUID(),"ADMIN"),403,"PERFIL_INVALIDO");
		var paciente=json(login(email,SENHA,UUID.randomUUID(),"PACIENTE"));
		var jwt=decoder.decode(texto(paciente,"accessToken"));
		assertThat(jwt.getClaimAsString("pid")).isEqualTo(pid.toString());
		assertThat(jwt.hasClaim("nid")).isFalse();
	}
	@Test void cookieExigeOriginPermitidaERecebeAtributosSeguros() throws Exception {
		var login=login(cadastrar(),SENHA,UUID.randomUUID(),null); String refresh=token(login);
		assertThat(login.headers().firstValue("Set-Cookie").orElseThrow()).contains("HttpOnly","Secure","SameSite=Strict","Path=/api/v1/auth");
		erro(cookie("/auth/refresh",refresh,null),403,"ACESSO_NEGADO");
		erro(cookie("/auth/refresh",refresh,"https://alheio.example.invalid"),403,"ACESSO_NEGADO");
		var resposta=cookie("/auth/refresh",refresh,"https://app.example.invalid");
		assertThat(resposta.statusCode()).isEqualTo(200);
		// BE-37: com a Origin do site o refresh vem só no cookie, nunca no corpo
		assertThat(json(resposta).path("refreshToken").isMissingNode()||json(resposta).path("refreshToken").isNull()).isTrue();
		String rotacionado=resposta.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0].substring("refreshToken=".length());
		var logout=cookie("/auth/logout",rotacionado,"https://app.example.invalid");
		assertThat(logout.statusCode()).isEqualTo(204);
		assertThat(logout.headers().firstValue("Set-Cookie").orElseThrow()).contains("Max-Age=0");
	}
	@Test void loginEmRefreshDoSiteNaoDevolvemRefreshNoCorpo() throws Exception {
		String email=cadastrar();
		var corpo=Map.of("email",email,"senha",SENHA,"dispositivo",Map.of("id",UUID.randomUUID(),"plataforma","WEB","nomeAparelho","Navegador"));
		var login=cliente.send(HttpRequest.newBuilder(uri("/auth/login")).header("Content-Type","application/json").header("Origin","https://app.example.invalid")
				.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(corpo))).build(),HttpResponse.BodyHandlers.ofString());
		assertThat(login.statusCode()).isEqualTo(200);
		assertThat(json(login).path("refreshToken").isMissingNode()||json(login).path("refreshToken").isNull()).isTrue();
		String cookie=login.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0].substring("refreshToken=".length());
		var renovada=cookie("/auth/refresh",cookie,"https://app.example.invalid");
		assertThat(renovada.statusCode()).isEqualTo(200);
		assertThat(json(renovada).path("refreshToken").isMissingNode()||json(renovada).path("refreshToken").isNull()).isTrue();
		assertThat(renovada.headers().firstValue("Set-Cookie").orElseThrow()).contains("refreshToken=","HttpOnly");
	}
	@Test void usuarioInativadoNaoPodeUsarRefreshOuSenhaCorreta() throws Exception {
		String email=cadastrar(); String token=token(login(email,SENHA,UUID.randomUUID(),null));
		jdbc.update("update usuario set ativo=false where email=?",email);
		erro(renovar(token),401,"TOKEN_INVALIDO");
		erro(login(email,SENHA,UUID.randomUUID(),null),401,"CREDENCIAIS_INVALIDAS");
	}

	String cadastrar() throws Exception {
		String email=UUID.randomUUID()+"@example.invalid";
		var resposta=post("/auth/cadastro-nutri",Map.of("nome","Nutri fictícia","email",email,"senha",SENHA,"crn","CRN-1 8814","aceitouTermos",true,"versaoTermos","2026-10"));
		assertThat(resposta.statusCode()).isEqualTo(201);return email;
	}
	HttpResponse<String> login(String email,String senha,UUID dispositivo,String perfil) throws Exception {
		var corpo=new HashMap<String,Object>();corpo.put("email",email);corpo.put("senha",senha);
		corpo.put("dispositivo",Map.of("id",dispositivo,"plataforma","ANDROID","nomeAparelho","Aparelho fictício"));
		if(perfil!=null)corpo.put("perfil",perfil);return post("/auth/login",corpo);
	}
	HttpResponse<String> post(String caminho,Object corpo) throws Exception {
		return cliente.send(HttpRequest.newBuilder(uri(caminho)).header("Content-Type","application/json")
				.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(corpo))).build(),HttpResponse.BodyHandlers.ofString());
	}
	HttpResponse<String> renovar(String token) throws Exception { return post("/auth/refresh",Map.of("refreshToken",token)); }
	HttpResponse<String> cookie(String caminho,String token,String origem) throws Exception {
		var pedido=HttpRequest.newBuilder(uri(caminho)).header("Cookie","refreshToken="+token).POST(HttpRequest.BodyPublishers.noBody());
		if(origem!=null)pedido.header("Origin",origem);return cliente.send(pedido.build(),HttpResponse.BodyHandlers.ofString());
	}
	HttpResponse<String> getEu(String authorization) throws Exception {
		return cliente.send(HttpRequest.newBuilder(uri("/eu")).header("Authorization",authorization).GET().build(),HttpResponse.BodyHandlers.ofString());
	}
	URI uri(String caminho) { return URI.create("http://localhost:"+porta+"/api/v1"+caminho); }
	JsonNode json(HttpResponse<String> resposta) { return mapper.readTree(resposta.body()); }
	String texto(JsonNode json,String campo) { return json.path(campo).asString(); }
	String token(HttpResponse<String> resposta) { assertThat(resposta.statusCode()).isEqualTo(200);return texto(json(resposta),"refreshToken"); }
	void erro(HttpResponse<String> resposta,int status,String codigo) {
		assertThat(resposta.statusCode()).isEqualTo(status);assertThat(texto(json(resposta),"codigo")).isEqualTo(codigo);
		assertThat(resposta.body()).doesNotContain(SENHA,"senha_hash","hash_token");
	}
}
