package com.br.dietamais.assinaturas.api;

import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AssinaturaResumoResposta(
		@NotNull UUID organizacaoId,
		@NotBlank String plano,
		@NotNull StatusAssinatura status,
		@NotNull @PositiveOrZero Integer limitePacientes,
		@NotNull @PositiveOrZero Integer pacientesOcupados,
		@NotNull Instant fimPeriodoEm) {
}
