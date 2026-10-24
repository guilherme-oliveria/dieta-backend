package com.br.dietamais.pacientes.api;

import java.time.Instant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ConviteResposta(
		@Valid @NotNull NutricionistaResumoResposta nutricionista,
		@Valid @NotNull TermoConsentimentoResposta termo,
		@NotNull Instant expiraEm) {
}
