package com.br.dietamais.sync.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SyncResposta(
		@NotBlank @Pattern(regexp = "[0-9]+") String cursor,
		@NotNull Boolean temMais,
		@Valid @NotNull MudancasFase1Resposta mudancas) {
}
