package com.br.dietamais.agenda.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/** Financeiro de uma consulta (BE-65, RN-FIN-01). Só a nutri vê; {@code versao} 0 = ainda não lançado. */
public record FinanceiroConsultaResposta(
		@NotNull UUID consultaId,
		@NotNull ComparecimentoConsulta comparecimento,
		@PositiveOrZero BigDecimal valor,
		FormaPagamento formaPagamento,
		@NotNull Boolean pago,
		@NotNull @PositiveOrZero Long versao) {
}
