package com.br.dietamais.comum.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.comum.aplicacao.RespostaIdempotente;
import com.br.dietamais.comum.aplicacao.ServicoIdempotencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.json.JsonMapper;

class FiltroIdempotenciaTest {
	private ServicoIdempotencia servico;
	private MockMvc mvc;
	private final AtomicInteger chamadas = new AtomicInteger();

	@BeforeEach
	void configurar() {
		servico = mock(ServicoIdempotencia.class);
		var controller = new Exemplo(chamadas);
		var fabrica = new FabricaProblema(JsonMapper.builder().build());
		var contexto = MockMvcBuilders.standaloneSetup(controller).build().getDispatcherServlet().getWebApplicationContext();
		var rotas = contexto.getBean(RequestMappingHandlerMapping.class);
		byte[] chave = new byte[32];
		new java.security.SecureRandom().nextBytes(chave);
		var cifrador = new CifradorResposta(java.util.Base64.getEncoder().encodeToString(chave));
		mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new TratadorErros(fabrica))
				.addFilters(new FiltroRastreio(), new FiltroIdempotencia(servico, fabrica, rotas, 1024, cifrador)).build();
		when(servico.executar(anyString(), any(), anyString(), any())).thenAnswer(invocacao -> {
			Supplier<RespostaIdempotente> operacao = invocacao.getArgument(3);
			return operacao.get();
		});
		SecurityContextHolder.clearContext();
	}

	@Test
	void corpoPodeSerLidoPeloControllerDepoisDoHash() throws Exception {
		mvc.perform(post("/api/v1/exemplo/criar").header("Idempotency-Key", UUID.randomUUID())
				.contentType("application/json").content("{\"nome\":\"Fictício\"}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Fictício"));
		assertThat(chamadas).hasValue(1);
	}

	@Test
	void chaveInvalidaECorpoMuitoGrandeSaoRejeitadosSemExecutar() throws Exception {
		mvc.perform(post("/api/v1/exemplo/criar").header("Idempotency-Key", "invalido")
				.contentType("application/json").content("{}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("FORMATO_INVALIDO"));
		mvc.perform(post("/api/v1/exemplo/criar").header("Idempotency-Key", UUID.randomUUID())
				.contentType("application/json").content("x".repeat(1025)))
				.andExpect(status().isBadRequest());
		assertThat(chamadas).hasValue(0);
		verifyNoInteractions(servico);
	}

	@Test
	void semChaveOuSemAnotacaoNaoAbreTransacaoDeIdempotencia() throws Exception {
		mvc.perform(post("/api/v1/exemplo/criar").contentType("application/json").content("{}"))
				.andExpect(status().isCreated());
		mvc.perform(post("/api/v1/exemplo/sem-chave").header("Idempotency-Key", UUID.randomUUID()))
				.andExpect(status().isNoContent());
		verifyNoInteractions(servico);
	}

	@Test
	void replayPreservaCorpoEHeaderFuncionalEUsaTraceDaNovaRequisicao() throws Exception {
		doReturn(new RespostaIdempotente(201,
				Map.of("Content-Type", java.util.List.of("application/json"), "Location", java.util.List.of("/api/v1/exemplo/123")),
				"{\"nome\":\"Resposta anterior\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
				.when(servico).executar(anyString(), any(), anyString(), any());
		mvc.perform(post("/api/v1/exemplo/criar").header("Idempotency-Key", UUID.randomUUID())
				.header("X-Trace-Id", "1111111111111111").contentType("application/json").content("{}"))
				.andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/exemplo/123"))
				.andExpect(header().string("X-Trace-Id", "1111111111111111"))
				.andExpect(jsonPath("$.nome").value("Resposta anterior"));
		assertThat(chamadas).hasValue(0);
	}

	@Test
	void falhaDaPersistenciaTambemUsaErroSeguroComRastreio() throws Exception {
		doThrow(new IllegalStateException("conteudo-interno")).when(servico)
				.executar(anyString(), any(), anyString(), any());
		mvc.perform(post("/api/v1/exemplo/criar").header("Idempotency-Key", UUID.randomUUID())
				.header("X-Trace-Id", "2222222222222222").contentType("application/json").content("{}"))
				.andExpect(status().isInternalServerError()).andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
				.andExpect(jsonPath("$.traceId").value("2222222222222222"))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("conteudo-interno"))));
		assertThat(chamadas).hasValue(0);
	}

	@Test
	void autenticacaoEPerfilDefinemEscoposDiferentesEQueryEntraNoHash() throws Exception {
		try {
			var chave = UUID.randomUUID();
			for (String perfil : java.util.List.of("NUTRICIONISTA", "PACIENTE")) {
				SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
						"usuario-ficticio", "", java.util.List.of(new SimpleGrantedAuthority(perfil))));
				mvc.perform(post("/api/v1/exemplo/criar?perfil=" + perfil).header("Idempotency-Key", chave)
						.contentType("application/json").content("{}"));
			}
			var escopos = ArgumentCaptor.forClass(String.class);
			var hashes = ArgumentCaptor.forClass(String.class);
			verify(servico, times(2)).executar(escopos.capture(), any(), hashes.capture(), any());
			assertThat(escopos.getAllValues()).doesNotHaveDuplicates().allMatch(valor -> valor.matches("[a-f0-9]{64}"));
			assertThat(hashes.getAllValues()).doesNotHaveDuplicates();
		} finally { SecurityContextHolder.clearContext(); }
	}

	@RestController
	static class Exemplo {
		private final AtomicInteger chamadas;
		Exemplo(AtomicInteger chamadas) { this.chamadas = chamadas; }
		@PostMapping("/api/v1/exemplo/criar") @Idempotente
		ResponseEntity<Map<String, String>> criar(@RequestBody Map<String, String> entrada) {
			chamadas.incrementAndGet();
			return ResponseEntity.status(201).body(entrada);
		}
		@PostMapping("/api/v1/exemplo/sem-chave")
		ResponseEntity<Void> semChave() { return ResponseEntity.noContent().build(); }
	}
}
