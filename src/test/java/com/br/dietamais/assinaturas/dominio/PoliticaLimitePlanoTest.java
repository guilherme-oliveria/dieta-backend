package com.br.dietamais.assinaturas.dominio;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.api.*;
import org.junit.jupiter.api.Test;

class PoliticaLimitePlanoTest {
	@Test void reservaExigeVagaDisponivelInclusiveConvites() {
		assertThatCode(()->PoliticaLimitePlano.validar(4,5)).doesNotThrowAnyException();
		for(int ocupados:new int[]{5,6})assertThatThrownBy(()->PoliticaLimitePlano.validar(ocupados,5))
			.isInstanceOfSatisfying(ExcecaoNegocio.class,erro->{assertThat(erro.codigo()).isEqualTo(CodigoErro.LIMITE_PACIENTES_ATINGIDO);assertThat(erro.regra()).isEqualTo("RN-ASS-03");});
	}
}
