package com.br.dietamais.contas.api;

import com.br.dietamais.comum.api.DispositivoRequisicao;

import com.br.dietamais.comum.api.Perfil;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequisicao(
		@NotBlank @Email String email,
		@NotBlank String senha,
		@Valid @NotNull DispositivoRequisicao dispositivo,
		@Schema(nullable = true) Perfil perfil) {
}
