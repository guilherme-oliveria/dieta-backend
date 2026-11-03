package com.br.dietamais.agenda.api;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

/** Consulta agendada pela nutri (RN-AGE-01). {@code pacienteId} é o id da ficha. {@code lugar}: endereço ou link da videochamada. */
public record ConsultaResposta(
		@NotNull UUID id,
		@NotNull UUID pacienteId,
		@NotNull UUID vinculoId,
		@NotNull Instant inicio,
		@NotNull @Positive Integer duracaoMin,
		@NotNull TipoConsulta tipo,
		String lugar,
		@NotNull StatusConsulta status,
		@NotNull @PositiveOrZero Long versao,
		@NotNull Instant criadoEm,
		@NotNull Instant atualizadoEm) {
}
