package com.br.dietamais.pacientes.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AtualizarPacienteRequisicao(
		@NotBlank @jakarta.validation.constraints.Size(max=150) String nome,
		@Schema(nullable = true) @Email @jakarta.validation.constraints.Size(max=255) String email,
		@Schema(nullable = true) LocalDate dataNascimento,
		@Schema(nullable = true) Sexo sexo,
		@Schema(nullable = true) @jakarta.validation.constraints.Size(max=30) String telefone,
		@Schema(nullable = true) @DecimalMin("0.5") @DecimalMax("2.5") @jakarta.validation.constraints.Digits(integer=1,fraction=3) BigDecimal estaturaReferencia,
		@Schema(nullable = true) @jakarta.validation.constraints.Size(max=2000) String objetivo,
		@Schema(nullable = true) @jakarta.validation.constraints.Size(max=10000) String observacoes,
		/** Ausente mantém o cadastro ampliado como está (cliente antigo não apaga); presente substitui, e objeto vazio limpa. */
		@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) @Schema(nullable = true) @jakarta.validation.Valid CadastroAmpliado cadastroAmpliado) {
	public AtualizarPacienteRequisicao(String nome,String email,LocalDate dataNascimento,Sexo sexo,String telefone,BigDecimal estaturaReferencia,String objetivo,String observacoes){this(nome,email,dataNascimento,sexo,telefone,estaturaReferencia,objetivo,observacoes,null);}
}
