package com.br.dietamais.agenda.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Resumo do período [de, ate) das consultas não canceladas da nutri (BE-65, RN-FIN-01). */
public record ResumoFinanceiroResposta(
		@NotNull Instant de,
		@NotNull Instant ate,
		@NotNull @PositiveOrZero Integer consultas,
		@NotNull @PositiveOrZero Integer compareceram,
		@NotNull @PositiveOrZero Integer faltaram,
		@NotNull @PositiveOrZero Integer pendentes,
		@NotNull @PositiveOrZero BigDecimal recebido,
		@NotNull @PositiveOrZero BigDecimal aReceber,
		@NotNull Map<FormaPagamento,BigDecimal> recebidoPorForma) {
}
