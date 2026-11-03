package com.br.dietamais.comum.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record ProblemaResposta(
		@NotNull URI type,
		@NotBlank String title,
		@NotNull Integer status,
		@NotBlank String detail,
		@NotNull CodigoErro codigo,
		@Schema(nullable = true) String regra,
		@Valid @NotNull List<ErroCampoResposta> campos,
		@Schema(nullable = true) String traceId,
		@Schema(nullable = true) JsonNode atual) {
}
