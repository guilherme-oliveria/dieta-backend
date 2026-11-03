package com.br.dietamais.agenda.api;

import jakarta.validation.constraints.*;
import java.time.Instant;

/** Campos ausentes ficam como estão. {@code lugar} vazio limpa o campo. {@code status} só aceita {@code CANCELADA}; consulta cancelada não muda mais. */
public record AtualizarConsultaRequisicao(
		Instant inicio,
		@Min(5) @Max(480) Integer duracaoMin,
		TipoConsulta tipo,
		@Size(max=200) String lugar,
		StatusConsulta status) {
}
