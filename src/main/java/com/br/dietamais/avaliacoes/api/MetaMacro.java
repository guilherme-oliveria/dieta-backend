package com.br.dietamais.avaliacoes.api;

import java.math.BigDecimal;

/** Faixa de um macronutriente em % do VET e em gramas; os máximos ficam ausentes quando só há mínimo. */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record MetaMacro(BigDecimal percentualMinimo,BigDecimal percentualMaximo,BigDecimal gramasMinimo,BigDecimal gramasMaximo) {
}
