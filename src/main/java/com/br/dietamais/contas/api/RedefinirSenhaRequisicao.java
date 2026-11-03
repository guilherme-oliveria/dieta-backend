package com.br.dietamais.contas.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequisicao(
		@NotBlank @Size(max = 4096) String token,
		@NotBlank @Size(min = 10) String novaSenha) {
	@Override public String toString() { return "RedefinirSenhaRequisicao[credenciais protegidas]"; }
}
