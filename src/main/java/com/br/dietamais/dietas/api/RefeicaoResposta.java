package com.br.dietamais.dietas.api;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record RefeicaoResposta(
	@NotNull UUID id,
	@NotBlank String nome,
	@NotNull LocalTime horario,
	@PositiveOrZero int ordem,
	@NotNull List<@NotNull @Valid ItemRefeicaoResposta> itens,
	@NotNull @Valid TotaisNutricionaisResposta totais) {
}
