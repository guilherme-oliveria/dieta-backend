package com.br.dietamais.contas.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import com.br.dietamais.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "IDEMPOTENCIA_CHAVE=")
@Import(TestcontainersConfiguration.class)
class CadastroNutriSemChaveApiTest {
	@Autowired JsonMapper mapper;
	@Autowired JdbcTemplate jdbc;
	@LocalServerPort int porta;

	@Test
	void falhaAoCifrarReplayDesfazContaOrganizacaoENutri() throws Exception {
		String email = "nutri-" + UUID.randomUUID() + "@example.invalid";
		var entrada = new CadastroNutriRequisicao("Nutri fictícia", email,
				"FRASE_FICTICIA_EXCLUSIVA_SEM_VALIDADE", "CRN-1 99999999", true, "2026-10", null);
		var chave = UUID.randomUUID();
		try (var cliente = HttpClient.newHttpClient()) {
			var requisicao = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + "/api/v1/auth/cadastro-nutri"))
					.timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json")
					.header("Idempotency-Key", chave.toString())
					.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(entrada))).build();
			var resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
			assertThat(resposta.statusCode()).isEqualTo(500);
			assertThat(mapper.readTree(resposta.body()).path("codigo").asString()).isEqualTo("ERRO_INTERNO");
		}
		for (String tabela : java.util.List.of("usuario", "organizacao", "nutricionista", "assinatura", "idempotencia")) {
			assertThat(jdbc.queryForObject("select count(*) from " + tabela, Long.class)).as(tabela).isZero();
		}
	}
}
