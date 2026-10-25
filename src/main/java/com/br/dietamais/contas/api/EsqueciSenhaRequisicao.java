package com.br.dietamais.contas.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EsqueciSenhaRequisicao(
		@NotBlank @Email @jakarta.validation.constraints.Size(max = 255) String email) {
	public EsqueciSenhaRequisicao {
		email = email == null ? null : email.strip().toLowerCase(java.util.Locale.ROOT);
	}
	@Override public String toString() { return "EsqueciSenhaRequisicao[e-mail protegido]"; }
}
