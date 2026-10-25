package com.br.dietamais.comum.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SessaoResposta(
		@Schema(nullable = true) String accessToken,
		@Schema(nullable = true) String refreshToken,
		@Schema(nullable = true) Instant expiraEm,
		@Valid @NotNull UsuarioResposta usuario) {
	@Override public String toString() { return "SessaoResposta[tokens e dados protegidos]"; }
}
