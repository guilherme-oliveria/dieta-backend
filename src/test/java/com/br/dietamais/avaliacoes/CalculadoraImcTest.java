package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.assertThat;
import com.br.dietamais.avaliacoes.api.ClassificacaoImc;
import com.br.dietamais.avaliacoes.dominio.CalculadoraImc;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculadoraImcTest {
	@Test void calculaComDuasCasasEClassificaPelaOms() {
		assertThat(CalculadoraImc.imc(new BigDecimal("70"),new BigDecimal("1.75"))).isEqualByComparingTo("22.86");
		assertThat(CalculadoraImc.classificar(new BigDecimal("18.49"))).isEqualTo(ClassificacaoImc.ABAIXO_DO_PESO);
		assertThat(CalculadoraImc.classificar(new BigDecimal("18.50"))).isEqualTo(ClassificacaoImc.NORMAL);
		assertThat(CalculadoraImc.classificar(new BigDecimal("25.00"))).isEqualTo(ClassificacaoImc.SOBREPESO);
		assertThat(CalculadoraImc.classificar(new BigDecimal("30.00"))).isEqualTo(ClassificacaoImc.OBESIDADE_I);
		assertThat(CalculadoraImc.classificar(new BigDecimal("35.00"))).isEqualTo(ClassificacaoImc.OBESIDADE_II);
		assertThat(CalculadoraImc.classificar(new BigDecimal("40.00"))).isEqualTo(ClassificacaoImc.OBESIDADE_III);
	}
}
