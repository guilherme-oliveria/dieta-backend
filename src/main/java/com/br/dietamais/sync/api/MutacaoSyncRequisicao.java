package com.br.dietamais.sync.api;

import com.br.dietamais.comum.api.OperacaoSync;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record MutacaoSyncRequisicao(
		@NotNull UUID idMutacao,
		@NotBlank @Size(max = 40) String entidade,
		@NotNull OperacaoSync operacao,
		@NotNull UUID id,
		@Schema(nullable = true) @PositiveOrZero Long versaoBase,
		@Schema(nullable = true, type = "object") JsonNode dados) {
}
