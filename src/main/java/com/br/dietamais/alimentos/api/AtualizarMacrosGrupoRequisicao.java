package com.br.dietamais.alimentos.api;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record AtualizarMacrosGrupoRequisicao(
	@NotNull @PositiveOrZero @Digits(integer=8,fraction=4) BigDecimal carboidratosPorPorcao,
	@NotNull @PositiveOrZero @Digits(integer=8,fraction=4) BigDecimal proteinasPorPorcao,
	@NotNull @PositiveOrZero @Digits(integer=8,fraction=4) BigDecimal gordurasPorPorcao) {}
