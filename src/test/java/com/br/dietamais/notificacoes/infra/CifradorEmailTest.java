package com.br.dietamais.notificacoes.infra;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CifradorEmailTest {
	static String chave() { byte[] bytes=new byte[32];new java.security.SecureRandom().nextBytes(bytes);return Base64.getEncoder().encodeToString(bytes); }
	@Test void cifraAutenticadaIsolaRegistroETipoComNonceAleatorio() {
		var cifra=new CifradorEmail(chave());var id=UUID.randomUUID();
		String texto="TOKEN_FICTICIO_SEM_VALIDADE ficticio@example.invalid";
		var a=cifra.cifrar(id,"CONFIRMAR_EMAIL",texto);var b=cifra.cifrar(id,"CONFIRMAR_EMAIL",texto);
		assertThat(a).isNotEqualTo(b).doesNotContain(texto);
		assertThat(cifra.decifrar(id,"CONFIRMAR_EMAIL",a)).isEqualTo(texto);
		assertThatThrownBy(()->cifra.decifrar(UUID.randomUUID(),"CONFIRMAR_EMAIL",a)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(()->cifra.decifrar(id,"REDEFINIR_SENHA",a)).isInstanceOf(IllegalStateException.class);
	}
	@Test void faltaOuFormatoInvalidoDeChaveNuncaPermiteTextoPuro() {
		assertThatThrownBy(()->new CifradorEmail("").cifrar(UUID.randomUUID(),"CONFIRMAR_EMAIL","TEXTO_FICTICIO")).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(()->new CifradorEmail("INVALIDA")).isInstanceOf(IllegalStateException.class);
	}
}
