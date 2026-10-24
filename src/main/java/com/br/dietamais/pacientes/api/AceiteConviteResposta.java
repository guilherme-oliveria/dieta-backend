package com.br.dietamais.pacientes.api;

import com.br.dietamais.contas.api.SessaoResposta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AceiteConviteResposta(
		@Valid @NotNull SessaoResposta sessao,
		@Valid @NotNull VinculoResposta vinculo) {
}
