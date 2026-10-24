package com.br.dietamais.contas.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EsqueciSenhaRequisicao(
		@NotBlank @Email String email) {
}
