package com.br.dietamais.contas.api;

import jakarta.validation.constraints.NotBlank;

public record ConfirmarEmailRequisicao(
		@NotBlank @jakarta.validation.constraints.Size(max = 4096) String token) {
	@Override public String toString() { return "ConfirmarEmailRequisicao[token protegido]"; }
}
