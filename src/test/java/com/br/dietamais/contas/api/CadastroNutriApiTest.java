package com.br.dietamais.contas.api;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.Executors;
import com.br.dietamais.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({ TestcontainersConfiguration.class, CadastroNutriApiTest.Configuracao.class })
class CadastroNutriApiTest {
	private static final Instant AGORA = Instant.parse("2026-10-25T12:00:00Z");
	private static final String CHAVE_TESTE = gerarChave();
	@Autowired JdbcTemplate jdbc;
	@Autowired JsonMapper mapper;
	@Autowired PasswordEncoder encoder;
	@LocalServerPort int porta;
	private final HttpClient cliente = HttpClient.newHttpClient();

	@org.junit.jupiter.api.AfterEach
	void encerrarCliente() { cliente.close(); }

	private static String gerarChave() {
		byte[] bytes = new byte[32];
		new SecureRandom().nextBytes(bytes);
		return Base64.getEncoder().encodeToString(bytes);
	}

	@DynamicPropertySource
	static void propriedades(DynamicPropertyRegistry registro) { registro.add("IDEMPOTENCIA_CHAVE", () -> CHAVE_TESTE); }

	@TestConfiguration(proxyBeanMethods = false)
	static class Configuracao {
		@Bean @Primary java.time.Clock relogioCadastro() { return java.time.Clock.fixed(AGORA, java.time.ZoneOffset.UTC); }
	}

	@Test
	void cadastroPublicoCriaIdentidadesEAssinaturaSemLoginAutomatico() throws Exception {
		var entrada = entrada();
		var resposta = cadastrar(mapper.writeValueAsString(entrada), null);
		assertThat(resposta.statusCode()).isEqualTo(201);
		var json = mapper.readTree(resposta.body());
		assertThat(json.path("usuario").path("email").asString()).isEqualTo(entrada.email());
		assertThat(json.path("usuario").path("emailConfirmado").asBoolean()).isFalse();
		assertThat(json.path("usuario").path("perfil").asString()).isEqualTo("NUTRICIONISTA");
		assertThat(json.path("usuario").path("fuso").asString()).isEqualTo("America/Sao_Paulo");
		assertThat(json.path("assinatura").path("status").asString()).isEqualTo("TESTE");
		assertThat(json.path("assinatura").path("fimPeriodoEm").asString()).isEqualTo(AGORA.plus(Duration.ofDays(30)).toString());
		assertThat(json.path("assinatura").path("pacientesOcupados").asInt()).isZero();
		assertThat(UUID.fromString(json.path("usuario").path("id").asString()).version()).isEqualTo(7);
		assertThat(UUID.fromString(json.path("usuario").path("nutricionistaId").asString()).version()).isEqualTo(7);
		assertThat(UUID.fromString(json.path("assinatura").path("organizacaoId").asString()).version()).isEqualTo(7);
		assertThat(resposta.body()).doesNotContain("senhaHash", "accessToken", "refreshToken", entrada.senha());
		assertThat(resposta.headers().firstValue("X-Trace-Id")).isPresent();
		assertThat(resposta.headers().allValues("Set-Cookie")).isEmpty();
		var usuario = jdbc.queryForMap("select * from usuario where email = ?", entrada.email());
		assertThat(encoder.matches(entrada.senha(), (String) usuario.get("senha_hash"))).isTrue();
		assertThat(usuario.get("senha_hash").toString()).startsWith("{argon2}$argon2id$");
		assertThat(usuario.get("versao_termos")).isEqualTo("2026-10");
		assertThat(jdbc.queryForObject("select termos_aceitos_em from usuario where email = ?", java.sql.Timestamp.class, entrada.email()).toInstant()).isEqualTo(AGORA);
		assertThat(jdbc.queryForObject("select o.tipo from organizacao o join nutricionista n on n.organizacao_id=o.id where n.usuario_id=?", String.class, usuario.get("id"))).isEqualTo("INDIVIDUAL");
		assertThat(jdbc.queryForObject("select crn_verificado from nutricionista where usuario_id=?", Boolean.class, usuario.get("id"))).isFalse();
	}

	@Test
	void duracaoDoTesteVemDoPlanoNoBanco() throws Exception {
		jdbc.update("update plano set duracao_dias=45 where codigo='TESTE'");
		try {
			var resposta = cadastrar(mapper.writeValueAsString(entrada()), null);
			assertThat(resposta.statusCode()).isEqualTo(201);
			assertThat(mapper.readTree(resposta.body()).path("assinatura").path("fimPeriodoEm").asString())
					.isEqualTo(AGORA.plus(Duration.ofDays(45)).toString());
		} finally { jdbc.update("update plano set duracao_dias=30 where codigo='TESTE'"); }
	}

	@Test
	void emailNormalizadoDuplicadoNaoDeixaOrganizacaoOuAssinaturaOrfa() throws Exception {
		var entrada = entrada();
		assertThat(cadastrar(mapper.writeValueAsString(entrada), null).statusCode()).isEqualTo(201);
		var contagens = contagens();
		var json = mapper.valueToTree(entrada);
		((tools.jackson.databind.node.ObjectNode) json).put("email", " " + entrada.email().toUpperCase(java.util.Locale.ROOT) + " ");
		var duplicado = cadastrar(mapper.writeValueAsString(json), null);
		assertThat(duplicado.statusCode()).isEqualTo(409);
		assertThat(mapper.readTree(duplicado.body()).path("codigo").asString()).isEqualTo("EMAIL_JA_CADASTRADO");
		assertThat(mapper.readTree(duplicado.body()).path("regra").asString()).isEqualTo("RN-CON-01");
		assertThat(contagens()).isEqualTo(contagens);
	}

	@Test
	void validacaoESenhaComumNaoCriamRegistros() throws Exception {
		var contagens = contagens();
		for (var par : java.util.List.of(java.util.Map.entry("senha", "1234567890"),
				java.util.Map.entry("crn", "invalido"), java.util.Map.entry("fuso", "Fuso/Inexistente"))) {
			var json = (tools.jackson.databind.node.ObjectNode) mapper.valueToTree(entrada());
			json.put(par.getKey(), par.getValue());
			var resposta = cadastrar(mapper.writeValueAsString(json), null);
			assertThat(resposta.statusCode()).isEqualTo(400);
			assertThat(mapper.readTree(resposta.body()).path("campos").get(0).path("campo").asString()).isEqualTo(par.getKey());
		}
		var termo = (tools.jackson.databind.node.ObjectNode) mapper.valueToTree(entrada());
		termo.put("aceitouTermos", false);
		var semAceite=cadastrar(mapper.writeValueAsString(termo), null);
		assertThat(semAceite.statusCode()).isEqualTo(422);
		assertThat(mapper.readTree(semAceite.body()).path("codigo").asString()).isEqualTo("TERMOS_NAO_ACEITOS");
		assertThat(contagens()).isEqualTo(contagens);
	}

	@Test
	void falhaDeConfiguracaoDoPlanoDesfazCadastroInteiro() throws Exception {
		var contagens = contagens();
		jdbc.update("update plano set ativo=false where codigo='TESTE'");
		try {
			var resposta = cadastrar(mapper.writeValueAsString(entrada()), null);
			assertThat(resposta.statusCode()).isEqualTo(500);
			assertThat(contagens()).isEqualTo(contagens);
		} finally { jdbc.update("update plano set ativo=true where codigo='TESTE'"); }
	}

	@Test
	void mesmaChaveIdempotenteRetornaMesmoCadastroSemDuplicar() throws Exception {
		String corpo = mapper.writeValueAsString(entrada());
		var chave = UUID.randomUUID();
		var primeira = cadastrar(corpo, chave);
		assertThat(primeira.statusCode()).isEqualTo(201);
		var contagens = contagens();
		var repetida = cadastrar(corpo, chave);
		assertThat(repetida.statusCode()).isEqualTo(201);
		assertThat(repetida.body()).isEqualTo(primeira.body());
		assertThat(contagens()).isEqualTo(contagens);
		assertThat(cadastrar(mapper.writeValueAsString(entrada()), chave).statusCode()).isEqualTo(409);
	}

	@Test
	void cadastrosConcorrentesDoMesmoEmailCriamUmaUnicaConta() throws Exception {
		var entrada = entrada();
		String corpo = mapper.writeValueAsString(entrada);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var tarefa = (java.util.concurrent.Callable<Integer>) () -> cadastrar(corpo, null).statusCode();
			var resultados = executor.invokeAll(java.util.List.of(tarefa, tarefa));
			assertThat(java.util.List.of(resultados.get(0).get(), resultados.get(1).get())).containsExactlyInAnyOrder(201, 409);
		}
		assertThat(jdbc.queryForObject("select count(*) from usuario where email=?", Long.class, entrada.email())).isEqualTo(1);
	}

	@Test
	void cadastroNaoReutilizaOrganizacaoInformadaPorOutroCliente() throws Exception {
		var primeiro = mapper.readTree(cadastrar(mapper.writeValueAsString(entrada()), null).body());
		String organizacao = primeiro.path("assinatura").path("organizacaoId").asString();
		var outro = (tools.jackson.databind.node.ObjectNode) mapper.valueToTree(entrada());
		outro.put("organizacaoId", organizacao);
		outro.put("perfil", "ADMIN");
		var resposta = cadastrar(mapper.writeValueAsString(outro), null);
		assertThat(resposta.statusCode()).isEqualTo(201);
		assertThat(mapper.readTree(resposta.body()).path("assinatura").path("organizacaoId").asString()).isNotEqualTo(organizacao);
		assertThat(mapper.readTree(resposta.body()).path("usuario").path("perfil").asString()).isEqualTo("NUTRICIONISTA");
		var consulta = cliente.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + "/api/v1/eu")).GET().build(), HttpResponse.BodyHandlers.ofString());
		assertThat(consulta.statusCode()).isEqualTo(401);
	}

	private CadastroNutriRequisicao entrada() {
		return new CadastroNutriRequisicao("Nutri fictícia", "nutri-" + UUID.randomUUID() + "@example.invalid",
				"FRASE_FICTICIA_EXCLUSIVA_SEM_VALIDADE", "CRN-1 99999999", true, "2026-10", null);
	}

	private HttpResponse<String> cadastrar(String corpo, UUID chave) throws Exception {
		var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + "/api/v1/auth/cadastro-nutri"))
				.timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8));
		if (chave != null) builder.header("Idempotency-Key", chave.toString());
		return cliente.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	private java.util.List<Long> contagens() {
		return java.util.List.of("usuario", "organizacao", "nutricionista", "assinatura").stream()
				.map(tabela -> jdbc.queryForObject("select count(*) from " + tabela, Long.class)).toList();
	}
}
