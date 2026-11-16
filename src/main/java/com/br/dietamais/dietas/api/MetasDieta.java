package com.br.dietamais.dietas.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Alvo da dieta: VET e distribuição de macronutrientes em % do VET (a soma não precisa fechar 100). */
public record MetasDieta(
	@NotNull @Positive @DecimalMax("20000") BigDecimal vetKcal,
	@NotNull @PositiveOrZero @DecimalMax("100") BigDecimal carboidratoPct,
	@NotNull @PositiveOrZero @DecimalMax("100") BigDecimal proteinaPct,
	@NotNull @PositiveOrZero @DecimalMax("100") BigDecimal lipidioPct) {
}
