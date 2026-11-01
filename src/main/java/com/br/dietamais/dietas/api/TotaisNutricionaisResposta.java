package com.br.dietamais.dietas.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record TotaisNutricionaisResposta(
	@NotNull @PositiveOrZero BigDecimal kcal,
	@NotNull @PositiveOrZero BigDecimal carboidratos,
	@NotNull @PositiveOrZero BigDecimal proteinas,
	@NotNull @PositiveOrZero BigDecimal gorduras,
	/** Só no total do dia e só quando a dieta tem metas (RN-DIE-10). */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Valid @Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) SaldoMetasResposta saldoMetas) {
	public TotaisNutricionaisResposta(BigDecimal kcal,BigDecimal carboidratos,BigDecimal proteinas,BigDecimal gorduras){this(kcal,carboidratos,proteinas,gorduras,null);}
	public TotaisNutricionaisResposta comSaldo(SaldoMetasResposta saldo){return new TotaisNutricionaisResposta(kcal,carboidratos,proteinas,gorduras,saldo);}
}
