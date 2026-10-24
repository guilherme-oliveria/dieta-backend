package com.br.dietamais.pacientes.api;

import java.net.URI;
import java.time.Instant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ConviteCriadoResposta(
		@NotBlank @Pattern(regexp = "[A-HJ-NP-Z2-9]{8}") String codigo,
		@NotNull URI link,
		@NotNull Instant expiraEm) {
}
