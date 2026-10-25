package com.br.dietamais.comum.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import com.br.dietamais.comum.infra.FiltroRastreio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import tools.jackson.databind.json.JsonMapper;

class ErrosApiTest {

	private MockMvc mvc;

	@BeforeEach
	void configurar() {
		mvc = MockMvcBuilders.standaloneSetup(new ControllerExemplo())
				.setControllerAdvice(new TratadorErros(new FabricaProblema(JsonMapper.builder().build())))
				.addFilters(new FiltroRastreio()).build();
	}

	@Test
	void regraDeNegocioViraProblemDetailComCodigoRegraERastreio() throws Exception {
		mvc.perform(get("/api/v1/exemplo/regra").header("X-Trace-Id", "4bf92f3577b34da6"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(header().string("X-Trace-Id", "4bf92f3577b34da6"))
				.andExpect(jsonPath("$.codigo").value("LIMITE_PACIENTES_ATINGIDO"))
				.andExpect(jsonPath("$.regra").value("RN-ASS-03"))
				.andExpect(jsonPath("$.campos").isEmpty())
				.andExpect(jsonPath("$.traceId").value("4bf92f3577b34da6"));
		assertThat(MDC.get("traceId")).isNull();
	}

	@Test
	void validacaoNaoExpoeValorRejeitado() throws Exception {
		mvc.perform(post("/api/v1/exemplo/validacao").contentType("application/json").content("{\"nome\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("OBRIGATORIO"))
				.andExpect(jsonPath("$.campos[0].campo").value("nome"))
				.andExpect(jsonPath("$.campos[0].codigo").value("OBRIGATORIO"))
				.andExpect(jsonPath("$.campos[0].rejectedValue").doesNotExist());
	}

	@Test
	void jsonInvalidoViraErroDeFormatoSemExporOCorpo() throws Exception {
		mvc.perform(post("/api/v1/exemplo/validacao").contentType("application/json").content("{invalido"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("FORMATO_INVALIDO"));
	}

	@Test
	void metodoETipoDeConteudoInvalidosNaoViramErroInterno() throws Exception {
		mvc.perform(get("/api/v1/exemplo/validacao")).andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.codigo").value("METODO_NAO_PERMITIDO"));
		mvc.perform(post("/api/v1/exemplo/validacao").contentType("text/plain").content("invalido"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.codigo").value("TIPO_CONTEUDO_NAO_SUPORTADO"));
	}

	@Test
	void falhaInesperadaNaoDevolveMensagemInterna() throws Exception {
		mvc.perform(get("/api/v1/exemplo/falha"))
				.andExpect(status().isInternalServerError()).andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("conteudo-interno"))));
	}

	@Test
	void traceInvalidoEsubstituidoERespostaDeSucessoTambemTemRastreio() throws Exception {
		var resposta = mvc.perform(get("/api/v1/exemplo/sucesso").header("X-Trace-Id", "invalido"))
				.andExpect(status().isOk()).andReturn().getResponse();
		assertThat(resposta.getHeader("X-Trace-Id")).matches("[0-9a-f]{32}");
		assertThat(MDC.get("traceId")).isNull();
	}

	@RestController
	@RequestMapping("/api/v1/exemplo")
	static class ControllerExemplo {
		@GetMapping("/regra")
		void regra() { throw new ExcecaoNegocio(CodigoErro.LIMITE_PACIENTES_ATINGIDO, "RN-ASS-03"); }
		@PostMapping("/validacao")
		void validar(@Valid @RequestBody Entrada entrada) { }
		@GetMapping("/falha")
		void falha() { throw new IllegalStateException("conteudo-interno"); }
		@GetMapping("/sucesso")
		Map<String, Boolean> sucesso() { return Map.of("ok", true); }
	}

	record Entrada(@NotBlank String nome) { }
}
