package com.br.dietamais.contas.api;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroNutriRequisicao(
		@NotBlank String nome,
		@NotBlank @Email String email,
		@NotBlank @Size(min = 10) String senha,
		@NotBlank @Pattern(regexp = "CRN-[1-9][0-9]* [0-9]+") String crn,
		@NotNull @AssertTrue Boolean aceitouTermos,
		@NotBlank String versaoTermos,
		@Schema(nullable = true) String fuso) {
}
