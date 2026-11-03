package com.br.dietamais.rotina.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Adesão numa janela de dias terminando hoje (fuso da nutri). Percentual é nulo quando não há refeição esperada. */
public record JanelaAdesao(
	@NotNull @PositiveOrZero Integer dias,
	@NotNull @PositiveOrZero Integer esperadas,
	@NotNull @PositiveOrZero Integer feitas,
	@NotNull @PositiveOrZero Integer trocadas,
	@NotNull @PositiveOrZero Integer puladas,
	@Schema(nullable = true) BigDecimal percentual) {
}
