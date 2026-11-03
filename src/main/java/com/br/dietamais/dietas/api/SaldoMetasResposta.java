package com.br.dietamais.dietas.api;

import java.math.BigDecimal;

/** Meta menos o total do dia; negativo = passou da meta. Carboidrato, proteína e lipídio em gramas. */
public record SaldoMetasResposta(BigDecimal kcal,BigDecimal carboidrato,BigDecimal proteina,BigDecimal lipidio) {
}
