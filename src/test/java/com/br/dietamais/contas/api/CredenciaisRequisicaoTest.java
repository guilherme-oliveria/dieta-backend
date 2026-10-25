package com.br.dietamais.contas.api;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.api.DispositivoRequisicao;
import com.br.dietamais.comum.api.Plataforma;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CredenciaisRequisicaoTest {
	@Test
	void loginNormalizaEmailSemAlterarSenha() {
		var entrada = new LoginRequisicao("  NUTRI@EXAMPLE.INVALID  ", "  frase fictícia preservada  ",
				new DispositivoRequisicao(UUID.randomUUID(), Plataforma.ANDROID, "Teste"), null);
		assertThat(entrada.email()).isEqualTo("nutri@example.invalid");
		assertThat(entrada.senha()).isEqualTo("  frase fictícia preservada  ");
	}

	@Test
	void representacoesTextuaisNaoExpoemCredenciaisOuEmail() {
		var entrada = new LoginRequisicao("ficticio@example.invalid", "CREDENCIAL_FICTICIA", null, null);
		assertThat(entrada.toString()).doesNotContain("CREDENCIAL_FICTICIA", "ficticio@example.invalid");
		assertThat(new RefreshRequisicao("TOKEN_SEM_VALIDADE").toString()).doesNotContain("TOKEN_SEM_VALIDADE");
		assertThat(new LogoutRequisicao("TOKEN_SEM_VALIDADE").toString()).doesNotContain("TOKEN_SEM_VALIDADE");
		assertThat(new com.br.dietamais.comum.api.SessaoResposta("ACCESS_SEM_VALIDADE", "REFRESH_SEM_VALIDADE", null, null).toString())
				.doesNotContain("ACCESS_SEM_VALIDADE", "REFRESH_SEM_VALIDADE");
	}
}
