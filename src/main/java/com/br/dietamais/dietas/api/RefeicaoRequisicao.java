package com.br.dietamais.dietas.api;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record RefeicaoRequisicao(
	UUID id,
	@NotBlank @Size(max=150) String nome,
	@NotNull LocalTime horario,
	@PositiveOrZero int ordem,
	@NotNull List<@NotNull @Valid ItemRefeicaoRequisicao> itens) {
}
