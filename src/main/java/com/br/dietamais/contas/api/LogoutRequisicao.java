package com.br.dietamais.contas.api;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequisicao(
		@NotBlank String refreshToken) {
}
