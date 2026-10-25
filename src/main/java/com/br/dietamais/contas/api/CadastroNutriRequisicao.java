package com.br.dietamais.contas.api;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroNutriRequisicao(
		@NotBlank @Size(max = 150) String nome,
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 10) String senha,
		@NotBlank @Size(max = 20) @Pattern(regexp = "CRN-[1-9][0-9]* [0-9]+") String crn,
		@NotNull @AssertTrue Boolean aceitouTermos,
		@NotBlank @Size(max = 40) String versaoTermos,
		@Schema(nullable = true) @Size(max = 80) String fuso) {

	public CadastroNutriRequisicao {
		nome = nome == null ? null : nome.strip();
		email = email == null ? null : email.strip().toLowerCase(java.util.Locale.ROOT);
		crn = crn == null ? null : crn.strip();
		versaoTermos = versaoTermos == null ? null : versaoTermos.strip();
		fuso = fuso == null ? null : fuso.strip();
	}

	@Override
	public String toString() { return "CadastroNutriRequisicao[conteúdo protegido]"; }
}
