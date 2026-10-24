package com.br.dietamais.contrato;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import jakarta.validation.Validation;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import com.br.dietamais.comum.api.ProblemaResposta;

class ContratoFase1Test {

	private final JsonMapper mapper = JsonMapper.builder()
			.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

	@Test
	void publicaExemplosDeTodosOsEndpointsDaFase1() throws Exception {
		assertThat(manifesto()).hasSize(17);
		assertThat(manifesto().stream().map(e -> e.metodo() + " " + e.caminho()))
				.doesNotHaveDuplicates();
	}

	@TestFactory
	Stream<DynamicTest> exemplosCorrespondemAosDtos() throws Exception {
		return manifesto().stream().flatMap(endpoint -> endpoint.exemplos().stream())
				.map(exemplo -> DynamicTest.dynamicTest(exemplo.arquivo(), () -> {
					var tipo = mapper.getTypeFactory().constructFromCanonical(exemplo.tipo());
					var json = recurso(exemplo.arquivo());
					Object dto = mapper.readValue(json, tipo);
					try (var factory = Validation.buildDefaultValidatorFactory()) {
						assertThat(factory.getValidator().validate(dto)).isEmpty();
					}
					assertThat(mapper.readTree(mapper.writeValueAsString(dto)))
							.isEqualTo(mapper.readTree(json));
				}));
	}

	@ParameterizedTest
	@CsvSource({
			"nome, ''", "email, email-invalido", "senha, curta", "crn, CRN-invalido" })
	void cadastroRejeitaCamposObrigatoriosOuFormatosInvalidos(String campo, String valor) throws Exception {
		var json = (tools.jackson.databind.node.ObjectNode) mapper.readTree(recurso("cadastrarNutri/requisicao.json"));
		json.put(campo, valor);
		var dto = mapper.treeToValue(json, com.br.dietamais.contas.api.CadastroNutriRequisicao.class);
		try (var factory = Validation.buildDefaultValidatorFactory()) {
			assertThat(factory.getValidator().validate(dto))
					.anySatisfy(erro -> assertThat(erro.getPropertyPath().toString()).isEqualTo(campo));
		}
	}

	@Test
	void contratoExigeAceiteExplicitoDosTermos() throws Exception {
		var json = (tools.jackson.databind.node.ObjectNode) mapper.readTree(recurso("aceitarConvite/requisicao.json"));
		json.put("aceitouTermo", false);
		var dto = mapper.treeToValue(json, com.br.dietamais.pacientes.api.AceitarConviteRequisicao.class);
		try (var factory = Validation.buildDefaultValidatorFactory()) {
			assertThat(factory.getValidator().validate(dto))
					.anySatisfy(erro -> assertThat(erro.getPropertyPath().toString()).isEqualTo("aceitouTermo"));
		}
	}

	@Test
	void exemplosDeErroUsamStatusERegrasDoCodigoPublicado() throws Exception {
		for (var endpoint : manifesto()) {
			for (var exemplo : endpoint.exemplos()) {
				if (exemplo.tipo().equals(ProblemaResposta.class.getName())) {
					var problema = mapper.readValue(recurso(exemplo.arquivo()), ProblemaResposta.class);
					assertThat(problema.status()).isEqualTo(problema.codigo().status());
					if (!problema.codigo().regras().isEmpty() && problema.regra() != null) {
						assertThat(problema.codigo().regras()).contains(problema.regra());
					}
				}
			}
		}
	}

	private List<Endpoint> manifesto() throws Exception {
		return mapper.readValue(recurso("manifesto.json"),
				mapper.getTypeFactory().constructCollectionType(List.class, Endpoint.class));
	}

	private String recurso(String arquivo) throws Exception {
		try (var input = getClass().getResourceAsStream("/contrato/" + arquivo)) {
			assertThat(input).as("Exemplo publicado: %s", arquivo).isNotNull();
			return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
	}

	record Endpoint(String metodo, String caminho, int status, List<Exemplo> exemplos) { }
	record Exemplo(String arquivo, String tipo) { }
}
