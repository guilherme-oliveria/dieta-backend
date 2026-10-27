package com.br.dietamais.rotina.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record CheckinRequisicao(
	UUID id,
	@NotNull UUID dietaId,
	@NotNull UUID refeicaoId,
	@NotNull LocalDate data,
	@NotNull StatusCheckin status,
	List<@NotNull @Valid TrocaCheckin> trocas,
	@NotNull Instant registradoEm) {
}
