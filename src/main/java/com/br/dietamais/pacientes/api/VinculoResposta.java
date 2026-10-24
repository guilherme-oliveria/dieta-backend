package com.br.dietamais.pacientes.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record VinculoResposta(
		@NotNull UUID id,
		@NotNull UUID pacienteId,
		@Valid @NotNull NutricionistaResumoResposta nutricionista,
		@NotNull StatusVinculo status,
		@Schema(nullable = true) Instant consentimentoEm,
		@Schema(nullable = true) Instant encerradoEm,
		@NotNull @PositiveOrZero Long versao,
		@NotNull Instant atualizadoEm) {
}
