package com.br.dietamais.pacientes.api;

import jakarta.validation.constraints.NotBlank;

public record TermoConsentimentoResposta(
		@NotBlank String versao,
		@NotBlank String texto) {
}
