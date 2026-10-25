package com.br.dietamais.contas.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;

class CadastroNutriRequisicaoTest {
	@Test
	void normalizaEmailENomeSemAlterarSenha() {
		var requisicao = new CadastroNutriRequisicao(" Nutri fictícia ", " NUTRI@EXAMPLE.INVALID ",
				"  FRASE_FICTICIA_SEM_VALIDADE  ", " CRN-1 99999999 ", true, " 2026-10 ", null);
		assertThat(requisicao.nome()).isEqualTo("Nutri fictícia");
		assertThat(requisicao.email()).isEqualTo("nutri@example.invalid");
		assertThat(requisicao.crn()).isEqualTo("CRN-1 99999999");
		assertThat(requisicao.versaoTermos()).isEqualTo("2026-10");
		assertThat(requisicao.senha()).isEqualTo("  FRASE_FICTICIA_SEM_VALIDADE  ");
	}

	@Test
	void representacaoTextualNaoExpoeCredencialNemDadosPessoais() {
		var requisicao = new CadastroNutriRequisicao("Nutri fictícia", "nutri@example.invalid",
				"FRASE_FICTICIA_SEM_VALIDADE", "CRN-1 99999999", true, "2026-10", null);
		assertThat(requisicao.toString()).doesNotContain(requisicao.senha(), requisicao.email());
	}

	@Test
	void nomeAcimaDoLimiteDaTabelaERecusadoAntesDePersistir() {
		var requisicao = new CadastroNutriRequisicao("a".repeat(151), "nutri@example.invalid",
				"FRASE_FICTICIA_SEM_VALIDADE", "CRN-1 99999999", true, "2026-10", null);
		try (var factory = Validation.buildDefaultValidatorFactory()) {
			assertThat(factory.getValidator().validate(requisicao)).anySatisfy(erro ->
					assertThat(erro.getPropertyPath().toString()).isEqualTo("nome"));
		}
	}
}
