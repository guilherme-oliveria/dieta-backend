package com.br.dietamais.contas.api;

import com.br.dietamais.comum.api.Plataforma;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarDispositivoRequisicao(
	@NotBlank @Size(max=4096) String token,
	@NotNull Plataforma plataforma,
	@Schema(nullable=true) @Size(max=150) String nomeAparelho) {}
