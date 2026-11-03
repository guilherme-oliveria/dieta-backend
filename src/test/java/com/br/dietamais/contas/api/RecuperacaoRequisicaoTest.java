package com.br.dietamais.contas.api;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

class RecuperacaoRequisicaoTest {
	@Test void representacaoTextualNaoContemTokenSenhaOuEmail() {
		assertThat(new ConfirmarEmailRequisicao("TOKEN_FICTICIO_SEM_VALIDADE").toString()).doesNotContain("TOKEN_FICTICIO_SEM_VALIDADE");
		assertThat(new RedefinirSenhaRequisicao("TOKEN_FICTICIO_SEM_VALIDADE","SENHA_FICTICIA_SEM_VALIDADE").toString())
				.doesNotContain("TOKEN_FICTICIO_SEM_VALIDADE","SENHA_FICTICIA_SEM_VALIDADE");
		assertThat(new EsqueciSenhaRequisicao("ficticio@example.invalid").toString()).doesNotContain("ficticio@example.invalid");
	}
	@Test void emailDaRecuperacaoUsaMesmaNormalizacaoDoCadastro() {
		assertThat(new EsqueciSenhaRequisicao("  FICTICIO@EXAMPLE.INVALID  ").email()).isEqualTo("ficticio@example.invalid");
	}
}
