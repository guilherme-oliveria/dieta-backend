package com.br.dietamais.comum.api;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DispositivoRequisicao(
		@NotNull UUID id,
		@NotNull Plataforma plataforma,
		@NotBlank String nomeAparelho) {
}
