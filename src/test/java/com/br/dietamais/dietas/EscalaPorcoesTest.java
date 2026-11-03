package com.br.dietamais.dietas;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.dominio.EscalaPorcoes;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** RN-MOD-02: escala das porções do modelo pelo alvo de energia, em passos de 0,5. */
class EscalaPorcoesTest {
	static BigDecimal n(String v){return new BigDecimal(v);}
	@Test void fatorEhAlvoSobreBase() {
		assertThat(EscalaPorcoes.fator(n("2000"),n("1600"))).isEqualByComparingTo("1.25");
		assertThat(EscalaPorcoes.fator(n("1800"),n("1800"))).isEqualByComparingTo("1");
	}
	@Test void fatorInviavelOuSemBaseEhNulo() {
		assertThat(EscalaPorcoes.fator(n("200"),n("2000"))).isNull();
		assertThat(EscalaPorcoes.fator(n("9000"),n("1500"))).isNull();
		assertThat(EscalaPorcoes.fator(n("1800"),null)).isNull();
		assertThat(EscalaPorcoes.fator(n("1800"),BigDecimal.ZERO)).isNull();
		assertThat(EscalaPorcoes.fator(null,n("1800"))).isNull();
	}
	@Test void arredondaParaMeiaPorcaoComMinimoDeMeia() {
		assertThat(EscalaPorcoes.escalar(n("2"),n("1.25"))).isEqualByComparingTo("2.5");
		assertThat(EscalaPorcoes.escalar(n("3"),n("1.1"))).isEqualByComparingTo("3.5");
		assertThat(EscalaPorcoes.escalar(n("1"),n("0.9"))).isEqualByComparingTo("1");
		assertThat(EscalaPorcoes.escalar(n("1"),n("0.3"))).isEqualByComparingTo("0.5");
		assertThat(EscalaPorcoes.escalar(n("0.5"),n("0.25"))).isEqualByComparingTo("0.5");
		assertThat(EscalaPorcoes.escalar(null,n("2"))).isNull();
	}
}
