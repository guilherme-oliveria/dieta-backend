package com.br.dietamais.pacientes.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CriarPacienteRequisicao(
		@Schema(nullable = true) UUID id,
		@NotBlank String nome,
		@Schema(nullable = true) @Email String email,
		@Schema(nullable = true) LocalDate dataNascimento,
		@Schema(nullable = true) Sexo sexo,
		@Schema(nullable = true) String telefone,
		@Schema(nullable = true) @DecimalMin("0.5") @DecimalMax("2.5") BigDecimal estaturaReferencia,
		@Schema(nullable = true) String objetivo,
		@Schema(nullable = true) String observacoes) {
}
