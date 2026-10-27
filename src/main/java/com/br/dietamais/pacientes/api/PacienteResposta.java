package com.br.dietamais.pacientes.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PacienteResposta(
		@NotNull UUID id,
		@NotNull UUID vinculoId,
		@NotBlank String nome,
		@Schema(nullable = true) @Email String email,
		@Schema(nullable = true) LocalDate dataNascimento,
		@Schema(nullable = true) Sexo sexo,
		@Schema(nullable = true) String telefone,
		@Schema(nullable = true) BigDecimal estaturaReferencia,
		@Schema(nullable = true) String objetivo,
		@Schema(nullable = true) String observacoes,
		@NotNull StatusVinculo status,
		@NotNull @PositiveOrZero Long versao,
		@NotNull Instant criadoEm,
		@NotNull Instant atualizadoEm,
		@Schema(nullable = true) BigDecimal adesao7d) {
	public PacienteResposta comAdesao7d(BigDecimal valor) {
		return new PacienteResposta(id,vinculoId,nome,email,dataNascimento,sexo,telefone,estaturaReferencia,objetivo,observacoes,status,versao,criadoEm,atualizadoEm,valor);
	}
}
