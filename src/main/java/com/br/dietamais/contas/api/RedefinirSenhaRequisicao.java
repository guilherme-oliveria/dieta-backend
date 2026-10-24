package com.br.dietamais.contas.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequisicao(
		@NotBlank String token,
		@NotBlank @Size(min = 10) String novaSenha) {
}
