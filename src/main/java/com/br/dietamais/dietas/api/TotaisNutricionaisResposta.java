package com.br.dietamais.dietas.api;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record TotaisNutricionaisResposta(
	@NotNull @PositiveOrZero BigDecimal kcal,
	@NotNull @PositiveOrZero BigDecimal carboidratos,
	@NotNull @PositiveOrZero BigDecimal proteinas,
	@NotNull @PositiveOrZero BigDecimal gorduras) {
}
