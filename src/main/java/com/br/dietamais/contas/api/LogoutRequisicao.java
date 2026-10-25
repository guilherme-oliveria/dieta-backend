package com.br.dietamais.contas.api;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequisicao(
		@NotBlank @jakarta.validation.constraints.Size(max = 4096) String refreshToken) {
	@Override public String toString() { return "LogoutRequisicao[token protegido]"; }
}
