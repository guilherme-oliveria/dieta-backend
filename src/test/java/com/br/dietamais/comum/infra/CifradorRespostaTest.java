package com.br.dietamais.comum.infra;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class CifradorRespostaTest {
	private CifradorResposta cifrador() {
		byte[] chave = new byte[32];
		new SecureRandom().nextBytes(chave);
		return new CifradorResposta(Base64.getEncoder().encodeToString(chave));
	}

	@Test
	void corpoSoPodeSerLidoComChaveEContextoCorretos() {
		var cifrador = cifrador();
		byte[] corpo = "{\"refreshToken\":\"FICTICIO_SEM_VALIDADE\"}".getBytes(StandardCharsets.UTF_8);
		byte[] cifrado = cifrador.cifrar(corpo, "usuario/chave/hash");
		assertThat(cifrado).isNotEqualTo(corpo);
		assertThat(new String(cifrado, StandardCharsets.UTF_8)).doesNotContain("FICTICIO_SEM_VALIDADE");
		assertThat(cifrador.decifrar(cifrado, "usuario/chave/hash")).isEqualTo(corpo);
	}

	@Test
	void nonceAleatorioImpedeCifrasIdenticasParaOMesmoCorpo() {
		var cifrador = cifrador();
		byte[] corpo = "ficticio".getBytes(StandardCharsets.UTF_8);
		assertThat(cifrador.cifrar(corpo, "contexto")).isNotEqualTo(cifrador.cifrar(corpo, "contexto"));
	}

	@Test
	void corrupcaoOuTrocaDeLinhaOuChaveNaoRevelaCorpo() {
		var cifrador = cifrador();
		byte[] cifrado = cifrador.cifrar("ficticio".getBytes(StandardCharsets.UTF_8), "linha-a");
		assertThatThrownBy(() -> cifrador.decifrar(cifrado, "linha-b")).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> cifrador().decifrar(cifrado, "linha-a")).isInstanceOf(IllegalStateException.class);
		cifrado[cifrado.length - 1] ^= 1;
		assertThatThrownBy(() -> cifrador.decifrar(cifrado, "linha-a")).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void ausenciaDeChaveNaoPermitePersistirEmTextoPuro() {
		assertThatThrownBy(() -> new CifradorResposta("").cifrar(new byte[0], "contexto"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void fingerprintDeRequisicaoDependeDeSegredoESemChaveNaoHaAssinatura() {
		byte[] requisicao = "credencial-ficticia".getBytes(StandardCharsets.UTF_8);
		var cifrador = cifrador();
		var assinatura = cifrador.autenticarRequisicao(requisicao);
		assertThat(assinatura).hasValueSatisfying(valor -> assertThat(valor).matches("[a-f0-9]{64}"));
		assertThat(cifrador.autenticarRequisicao(requisicao)).isEqualTo(assinatura);
		assertThat(cifrador().autenticarRequisicao(requisicao)).isNotEqualTo(assinatura);
		assertThat(new CifradorResposta("").autenticarRequisicao(requisicao)).isEmpty();
	}
}
