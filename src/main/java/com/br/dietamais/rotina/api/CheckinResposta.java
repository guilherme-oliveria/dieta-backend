package com.br.dietamais.rotina.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record CheckinResposta(
	@NotNull UUID id,
	@NotNull UUID pacienteId,
	@NotNull UUID dietaId,
	@NotNull UUID refeicaoId,
	@NotNull LocalDate data,
	@NotNull StatusCheckin status,
	@NotNull List<@NotNull @Valid TrocaCheckin> trocas,
	@NotNull Instant registradoEm) {
}
