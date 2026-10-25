package com.br.dietamais.contas.dominio;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import com.br.dietamais.comum.api.CodigoErro;
import com.br.dietamais.comum.api.ExcecaoNegocio;
import org.junit.jupiter.api.Test;

class PoliticaSenhaTest {
	private final PoliticaSenha politica = new PoliticaSenha(Set.of(
			"c775e7b757ede630cd0aa1113bd102661ab38829ca52a6422ab782862f268646"));

	@Test
	void senhaCurtaOuComumERecusadaComRnCon02SemExporValor() {
		for (String senha : java.util.List.of("curta", "1234567890", " 1234567890 ")) {
			assertThatThrownBy(() -> politica.validar(senha)).isInstanceOfSatisfying(ExcecaoNegocio.class, erro -> {
				assertThat(erro.codigo()).isEqualTo(CodigoErro.FORMATO_INVALIDO);
				assertThat(erro.regra()).isEqualTo("RN-CON-02");
				assertThat(erro.campos().getFirst().campo()).isEqualTo("senha");
				assertThat(erro.getMessage()).doesNotContain(senha);
			});
		}
	}

	@Test
	void fraseLongaNaoListadaEPermitidaSemRegraDeComposicaoArtificial() {
		assertThatCode(() -> politica.validar("frase fictícia exclusiva para teste de domínio"))
				.doesNotThrowAnyException();
	}
}
