package com.br.dietamais.dietas.api;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record DietasPacienteResposta(
	UUID dietaEmUsoId,
	@NotNull @PositiveOrZero Long versao,
	@NotNull List<@NotNull @Valid DietaSnapshotResposta> itens) {
}
