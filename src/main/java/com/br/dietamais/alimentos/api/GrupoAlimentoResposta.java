package com.br.dietamais.alimentos.api;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record GrupoAlimentoResposta(
	@NotNull UUID id,
	@NotBlank String nome,
	@NotNull @PositiveOrZero BigDecimal kcalPorPorcao,
	@NotNull @PositiveOrZero BigDecimal carboidratosPorPorcao,
	@NotNull @PositiveOrZero BigDecimal proteinasPorPorcao,
	@NotNull @PositiveOrZero BigDecimal gordurasPorPorcao) {
}
