package com.br.dietamais.contas.infra;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import com.br.dietamais.comum.api.ExcecaoNegocio;

class SegurancaContasConfigTest {
	@Test
	void usaArgon2idComSaltAleatorioSemAlterarSenha() {
		var encoder = new SegurancaContasConfig().codificadorSenha();
		String senha = "  FRASE_FICTICIA_EXCLUSIVA_SEM_VALIDADE  ";
		String hash = encoder.encode(senha);
		assertThat(hash).startsWith("{argon2}$argon2id$").doesNotContain(senha);
		assertThat(encoder.matches(senha, hash)).isTrue();
		assertThat(encoder.matches(senha.strip(), hash)).isFalse();
		assertThat(encoder.encode(senha)).isNotEqualTo(hash);
	}

	@Test
	void listaLocalPadraoBloqueiaSenhaComumSemConsultaExterna() throws Exception {
		var politica = new SegurancaContasConfig().politicaSenha(
				new ClassPathResource("seguranca/senhas-comuns-sha256.txt"));
		assertThatThrownBy(() -> politica.validar("password123")).isInstanceOf(ExcecaoNegocio.class);
		assertThatCode(() -> politica.validar("FRASE_FICTICIA_EXCLUSIVA_SEM_VALIDADE")).doesNotThrowAnyException();
	}
}
