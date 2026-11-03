package com.br.dietamais.comum.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UsuarioResposta(
		@NotNull UUID id,
		@NotBlank String nome,
		@NotBlank @Email String email,
		@NotNull Boolean emailConfirmado,
		@NotEmpty List<Perfil> perfis,
		@Schema(nullable = true) Perfil perfil,
		@Schema(nullable = true) UUID nutricionistaId,
		@Schema(nullable = true) UUID pacienteId,
		@NotBlank String fuso) {
}
