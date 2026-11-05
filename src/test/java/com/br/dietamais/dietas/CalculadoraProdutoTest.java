package com.br.dietamais.dietas;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.alimentos.api.ProdutoParaDieta;
import com.br.dietamais.dietas.dominio.CalculadoraTotais;
import com.br.dietamais.referencias.api.PorcoesGrupos;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

/** BE-48c: totais do item por produto (nutrientes por 100 g × gramas, 2 casas HALF_UP) e tradução das chaves do produto para os códigos da dieta. */
class CalculadoraProdutoTest {
	@Test void totaisProporcionaisAosGramas() {
		var nutrientes=new HashMap<String,BigDecimal>();nutrientes.put("energia_kcal",new BigDecimal("380"));nutrientes.put("carboidrato_g",new BigDecimal("8.5"));
		nutrientes.put("proteina_g",new BigDecimal("78"));nutrientes.put("lipideos_g",new BigDecimal("4.2"));
		var p=new ProdutoParaDieta(UUID.randomUUID(),"Whey","colher",true,ProdutoParaDieta.paraCodigos(nutrientes));
		var t=CalculadoraTotais.produto(p,new BigDecimal("50"));
		assertThat(t.kcal()).isEqualByComparingTo("190.00");assertThat(t.carboidratos()).isEqualByComparingTo("4.25");assertThat(t.proteinas()).isEqualByComparingTo("39.00");assertThat(t.gorduras()).isEqualByComparingTo("2.10");
		assertThat(CalculadoraTotais.produto(p,new BigDecimal("33.33")).carboidratos()).isEqualByComparingTo("2.83");
	}
	@Test void chavesDoProdutoViramCodigosDaDietaComAMesmaUnidade() {
		var n=Map.of("sodio_mg",new BigDecimal("210"),"cobre_ug",new BigDecimal("12"),"vitamina_d_ug",new BigDecimal("5"),"taurina_mg",new BigDecimal("7"));
		var c=ProdutoParaDieta.paraCodigos(n);
		assertThat(c).containsOnlyKeys("na","cu","d");
		// toda chave traduzida existe na lista de nutrientes da dieta
		assertThat(PorcoesGrupos.UNIDADES.keySet()).containsAll(c.keySet());
	}
}
