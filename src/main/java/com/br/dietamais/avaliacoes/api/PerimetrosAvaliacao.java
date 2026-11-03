package com.br.dietamais.avaliacoes.api;

import java.math.BigDecimal;

/** Perímetros em cm (RN-AVA-01/03: 10 a 250). Todos opcionais. */
public record PerimetrosAvaliacao(BigDecimal pescoco,BigDecimal torax,BigDecimal cintura,BigDecimal abdomen,BigDecimal quadril,
		BigDecimal braco,BigDecimal antebraco,BigDecimal coxa,BigDecimal panturrilha) {
	public static final PerimetrosAvaliacao VAZIO=new PerimetrosAvaliacao(null,null,null,null,null,null,null,null,null);
}
