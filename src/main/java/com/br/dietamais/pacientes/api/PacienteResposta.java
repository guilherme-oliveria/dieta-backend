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
		@Schema(nullable = true) BigDecimal adesao7d,
		/** Cadastro ampliado (BE-64); ausente quando a nutri não preencheu nada. */
		@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
		@Schema(nullable = true,requiredMode = Schema.RequiredMode.NOT_REQUIRED) CadastroAmpliado cadastroAmpliado,
		/** Início da próxima consulta agendada (BE-66); só na lista da nutri. */
		@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
		@Schema(nullable = true,requiredMode = Schema.RequiredMode.NOT_REQUIRED) Instant proximaConsulta,
		/** Avaliação mais recente (BE-66); só na lista da nutri. */
		@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
		@Schema(nullable = true,requiredMode = Schema.RequiredMode.NOT_REQUIRED) UltimaAvaliacaoResumo ultimaAvaliacao) {
	public PacienteResposta(UUID id,UUID vinculoId,String nome,String email,LocalDate dataNascimento,Sexo sexo,String telefone,BigDecimal estaturaReferencia,String objetivo,String observacoes,StatusVinculo status,Long versao,Instant criadoEm,Instant atualizadoEm,BigDecimal adesao7d,CadastroAmpliado cadastroAmpliado){
		this(id,vinculoId,nome,email,dataNascimento,sexo,telefone,estaturaReferencia,objetivo,observacoes,status,versao,criadoEm,atualizadoEm,adesao7d,cadastroAmpliado,null,null);
	}
	public PacienteResposta(UUID id,UUID vinculoId,String nome,String email,LocalDate dataNascimento,Sexo sexo,String telefone,BigDecimal estaturaReferencia,String objetivo,String observacoes,StatusVinculo status,Long versao,Instant criadoEm,Instant atualizadoEm,BigDecimal adesao7d){
		this(id,vinculoId,nome,email,dataNascimento,sexo,telefone,estaturaReferencia,objetivo,observacoes,status,versao,criadoEm,atualizadoEm,adesao7d,null);
	}
	public PacienteResposta comAdesao7d(BigDecimal valor) {
		return new PacienteResposta(id,vinculoId,nome,email,dataNascimento,sexo,telefone,estaturaReferencia,objetivo,observacoes,status,versao,criadoEm,atualizadoEm,valor,cadastroAmpliado,proximaConsulta,ultimaAvaliacao);
	}
	public PacienteResposta comResumo(Instant proxima,UltimaAvaliacaoResumo ultima) {
		return new PacienteResposta(id,vinculoId,nome,email,dataNascimento,sexo,telefone,estaturaReferencia,objetivo,observacoes,status,versao,criadoEm,atualizadoEm,adesao7d,cadastroAmpliado,proxima,ultima);
	}
}
