package com.br.dietamais.pacientes.dominio;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.api.*;
import org.junit.jupiter.api.Test;

class TermoPublicadoTest {
	@Test void naoInventaConsentimentoSemPublicacao() {
		for(String[] entrada:new String[][]{{"",""},{"v1",""},{"","Texto fictício"}})
			assertThatThrownBy(()->TermoPublicado.exigir(entrada[0],entrada[1])).isInstanceOfSatisfying(ExcecaoNegocio.class,
				erro->assertThat(erro.codigo()).isEqualTo(CodigoErro.CONSENTIMENTO_OBRIGATORIO));
		assertThat(TermoPublicado.exigir("teste-v1","Termo fictício apenas para teste").versao()).isEqualTo("teste-v1");
	}
}
