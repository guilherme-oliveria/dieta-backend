package com.br.dietamais.comum.api;

import jakarta.validation.constraints.NotBlank;

public record ErroCampoResposta(
		@NotBlank String campo,
		@NotBlank String codigo,
		@NotBlank String mensagem) {
}
