package com.br.dietamais.dietas;

import static org.assertj.core.api.Assertions.assertThat;
import com.br.dietamais.alimentos.api.AlimentoParaDieta;
import com.br.dietamais.dietas.dominio.CalculadoraTotais;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CalculadoraTotaisTest {
	AlimentoParaDieta pao=new AlimentoParaDieta(UUID.randomUUID(),UUID.randomUUID(),"Pão fictício","1 fatia",new BigDecimal("50"),true,
		new BigDecimal("150"),new BigDecimal("28.3333"),new BigDecimal("4.5"),new BigDecimal("1.25"));
	@Test void itemMultiplicaPorcoesEArredondaMeioParaCima() {
		var t=CalculadoraTotais.item(pao,new BigDecimal("1.5"));
		assertThat(t.kcal()).isEqualByComparingTo("225.00");assertThat(t.carboidratos()).isEqualByComparingTo("42.50");
		assertThat(t.proteinas()).isEqualByComparingTo("6.75");assertThat(t.gorduras()).isEqualByComparingTo("1.88");
	}
	@Test void somaUsaOsItensJaArredondadosEZeroParaVazio() {
		var a=CalculadoraTotais.item(pao,new BigDecimal("1"));var b=CalculadoraTotais.item(pao,new BigDecimal("2"));
		assertThat(CalculadoraTotais.somar(List.of(a,b)).kcal()).isEqualByComparingTo("450.00");
		assertThat(CalculadoraTotais.somar(List.of())).isEqualTo(CalculadoraTotais.ZERO);
		assertThat(CalculadoraTotais.gramas(pao,new BigDecimal("1.5"))).isEqualByComparingTo("75.00");
	}
}
