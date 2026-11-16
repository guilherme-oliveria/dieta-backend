package com.br.dietamais.agenda.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Substitui o financeiro da consulta (PUT). {@code pago} exige valor e forma de pagamento (RN-FIN-01). */
public record AtualizarFinanceiroConsultaRequisicao(
		@NotNull ComparecimentoConsulta comparecimento,
		@PositiveOrZero @Digits(integer=8,fraction=2) BigDecimal valor,
		FormaPagamento formaPagamento,
		@NotNull Boolean pago) {
}
