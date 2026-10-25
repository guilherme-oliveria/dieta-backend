package com.br.dietamais.pacientes.dominio;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.api.*;
import org.junit.jupiter.api.Test;

class VersaoFichaTest {
	@Test void aceitaSomenteUmaVersaoForteNaoNegativa() {
		assertThat(VersaoFicha.ler("\"0\"")).isZero();
		assertThat(VersaoFicha.ler("\"42\"")).isEqualTo(42);
		for(String entrada:new String[]{null,"","*","W/\"1\"","1","\"-1\"","\"1\",\"2\"","\"999999999999999999999999\""})
			assertThatThrownBy(()->VersaoFicha.ler(entrada)).isInstanceOfSatisfying(ExcecaoNegocio.class,
				erro->assertThat(erro.codigo()).isEqualTo(CodigoErro.FORMATO_INVALIDO));
	}
}
