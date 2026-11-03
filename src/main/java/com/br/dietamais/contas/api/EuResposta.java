package com.br.dietamais.contas.api;

import com.br.dietamais.comum.api.UsuarioResposta;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import com.br.dietamais.assinaturas.api.AssinaturaResumoResposta;
import com.br.dietamais.pacientes.api.VinculoResposta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record EuResposta(
		@Valid @NotNull UsuarioResposta usuario,
		@Schema(nullable = true) @Valid AssinaturaResumoResposta assinatura,
		@Valid @NotNull List<VinculoResposta> vinculos) {
}
