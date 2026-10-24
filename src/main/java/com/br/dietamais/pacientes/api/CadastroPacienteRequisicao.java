package com.br.dietamais.pacientes.api;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroPacienteRequisicao(
		@NotBlank String nome,
		@NotBlank @Email String email,
		@NotBlank @Size(min = 10) String senha,
		@Schema(nullable = true) String fuso) {
}
