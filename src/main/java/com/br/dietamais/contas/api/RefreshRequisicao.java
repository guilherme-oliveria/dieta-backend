package com.br.dietamais.contas.api;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequisicao(
		@NotBlank @jakarta.validation.constraints.Size(max = 4096) String refreshToken) {
	@Override public String toString() { return "RefreshRequisicao[token protegido]"; }
}
