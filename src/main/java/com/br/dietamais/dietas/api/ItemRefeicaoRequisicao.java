package com.br.dietamais.dietas.api;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record ItemRefeicaoRequisicao(
	UUID id,
	@NotNull UUID alimentoId,
	@NotNull @Positive BigDecimal porcoes,
	@Size(max=2000) String observacao) {
}
