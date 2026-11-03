package com.br.dietamais.sync.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MudancaSyncResposta<T>(
		@NotNull UUID id,
		@NotNull @PositiveOrZero Long versao,
		@NotNull Boolean excluido,
		@Schema(nullable = true) @Valid T dados) {
}
