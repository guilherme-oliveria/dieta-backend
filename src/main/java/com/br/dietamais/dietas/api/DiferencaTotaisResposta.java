package com.br.dietamais.dietas.api;

import java.math.BigDecimal;

/** Total da alternativa menos o da refeição que ela substitui; negativo = menos que a principal. */
public record DiferencaTotaisResposta(BigDecimal kcal,BigDecimal carboidratos,BigDecimal proteinas,BigDecimal gorduras) {
}
