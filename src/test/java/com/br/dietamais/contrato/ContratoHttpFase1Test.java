package com.br.dietamais.contrato;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.br.dietamais.config.OpenApiConfig;
import com.br.dietamais.config.SecurityConfig;
import com.br.dietamais.contas.api.AuthController;
import com.br.dietamais.contas.api.EuController;
import com.br.dietamais.pacientes.api.ConviteController;
import com.br.dietamais.pacientes.api.PacienteController;
import com.br.dietamais.sync.api.SyncController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = ContratoHttpFase1Test.AplicacaoContrato.class, properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
		"spring.docker.compose.enabled=false", "springdoc.swagger-ui.enabled=false" })
class ContratoHttpFase1Test {
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.br.dietamais.contas.aplicacao.ConsultarEu consultaEu;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.br.dietamais.pacientes.aplicacao.GerenciarPacientes pacientes;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.br.dietamais.contas.aplicacao.CadastrarNutri cadastro;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.br.dietamais.contas.aplicacao.AutenticarSessao sessoes;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	org.springframework.security.oauth2.jwt.JwtDecoder decoder;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.br.dietamais.contas.aplicacao.GerenciarEmailSenha emails;
	private static final java.util.Set<String> IMPLEMENTADOS = java.util.Set.of("/api/v1/auth/cadastro-nutri",
			"/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout",
			"/api/v1/auth/confirmar-email", "/api/v1/auth/esqueci-senha", "/api/v1/auth/redefinir-senha", "/api/v1/eu",
			"/api/v1/pacientes", "/api/v1/pacientes/{id}");

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@Import({ AuthController.class, EuController.class, ConviteController.class, PacienteController.class,
			SyncController.class, OpenApiConfig.class, SecurityConfig.class, com.br.dietamais.comum.api.FabricaProblema.class,
			com.br.dietamais.contas.api.TransporteSessao.class, com.br.dietamais.comum.infra.RelogioConfig.class })
	static class AplicacaoContrato {
		@Bean
		UserDetailsService semCredenciaisReais() {
			return username -> { throw new UsernameNotFoundException("Teste de contrato sem credenciais"); };
		}
	}

	@Autowired
	WebApplicationContext contexto;

	MockMvc mvc;

	@BeforeEach
	void configurarSegurancaDoMockMvc() {
		mvc = webAppContextSetup(contexto).apply(springSecurity()).build();
	}

	@Autowired
	JsonMapper mapper;

	@Test
	@WithMockUser
	void endpointsPendentesRespondem501SemExecutarRegrasOuExporDados() throws Exception {
		var manifesto = mapper.readTree(recurso("manifesto.json"));
		int pendentes = 0;
		for (var endpoint : manifesto) {
			if (IMPLEMENTADOS.contains(endpoint.get("caminho").asString())) continue;
			pendentes++;
			String caminho = endpoint.get("caminho").asText()
					.replace("{id}", "019a0f32-5000-7000-8000-000000000003")
					.replace("{codigo}", "AB2CD3EF");
			var requisicao = request(HttpMethod.valueOf(endpoint.get("metodo").asText()), caminho)
					.header("If-Match", "\"1\"");
			for (var exemplo : endpoint.get("exemplos")) {
				if (exemplo.get("arquivo").asText().endsWith("/requisicao.json")) {
					requisicao.contentType("application/json").content(recurso(exemplo.get("arquivo").asText()));
				}
			}
			var resposta = mvc.perform(requisicao).andExpect(status().isNotImplemented()).andReturn().getResponse();
			assertThat(resposta.getContentType()).startsWith("application/problem+json");
			assertThat(mapper.readTree(resposta.getContentAsString(StandardCharsets.UTF_8)))
					.as("%s %s", endpoint.get("metodo"), caminho)
					.isEqualTo(mapper.readTree(recurso("nao-implementado.json")));
		}
		assertThat(pendentes).isEqualTo(5);
	}

	@Test
	@WithMockUser
	void openApiPublicaCadaOperacaoESeusSchemasDeSucessoEFalha() throws Exception {
		String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		var openApi = mapper.readTree(json);
		for (var endpoint : mapper.readTree(recurso("manifesto.json"))) {
			var operacao = openApi.path("paths").path(endpoint.get("caminho").asText())
					.path(endpoint.get("metodo").asText().toLowerCase(java.util.Locale.ROOT));
			assertThat(operacao.isMissingNode()).isFalse();
			String sucesso = endpoint.get("status").asText();
			assertThat(operacao.path("responses").has(sucesso)).isTrue();
			if (!IMPLEMENTADOS.contains(endpoint.get("caminho").asString())) {
				assertThat(operacao.path("responses").has("501")).isTrue();
			}
		}
		assertThat(openApi.path("components").path("schemas").propertyNames())
				.containsAll(List.of("SessaoResposta", "EuResposta", "PacienteResposta", "SyncResposta", "ProblemaResposta"));
	}

	@Test
	void rotasExigemBearerENaoAceitamBasic() throws Exception {
		mvc.perform(get("/api/v1/eu")).andExpect(status().isUnauthorized())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.codigo").value("NAO_AUTENTICADO"));
		mvc.perform(get("/api/v1/eu").header("Authorization", "Basic " + java.util.Base64.getEncoder()
				.encodeToString("ficticio:SEM_VALIDADE".getBytes(StandardCharsets.UTF_8))))
				.andExpect(status().isUnauthorized())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.codigo").value("NAO_AUTENTICADO"));
	}

	private String recurso(String arquivo) throws Exception {
		try (var input = getClass().getResourceAsStream("/contrato/" + arquivo)) {
			assertThat(input).isNotNull();
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
