package com.br.dietamais.pacientes.api;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NutricionistaResumoResposta(
		@NotNull UUID id,
		@NotBlank String nome,
		@NotBlank String crn) {
}
