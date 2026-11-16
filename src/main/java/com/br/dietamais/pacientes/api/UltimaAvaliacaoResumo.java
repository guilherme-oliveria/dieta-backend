package com.br.dietamais.pacientes.api;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Resumo da avaliação mais recente do paciente para o cartão "Última avaliação" da lista (BE-66). */
public record UltimaAvaliacaoResumo(
		@NotNull java.util.UUID id,
		@NotNull Instant dataHora,
		@NotNull BigDecimal pesoKg,
		@NotNull BigDecimal imc,
		@NotBlank String classificacaoImc) {
}
