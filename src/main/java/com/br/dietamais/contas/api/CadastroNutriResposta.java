package com.br.dietamais.contas.api;

import com.br.dietamais.comum.api.UsuarioResposta;

import com.br.dietamais.assinaturas.api.AssinaturaResumoResposta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CadastroNutriResposta(
		@Valid @NotNull UsuarioResposta usuario,
		@Valid @NotNull AssinaturaResumoResposta assinatura) {
}
