package com.br.dietamais.dietas.api;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record ItemRefeicaoResposta(
	@NotNull UUID id,
	@NotNull UUID alimentoId,
	@NotNull UUID grupoId,
	@NotBlank String nomeAlimento,
	@NotNull @Positive BigDecimal porcoes,
	@NotBlank String medidaCaseira,
	@NotNull @Positive BigDecimal gramas,
	String observacao,
	@NotNull @Valid TotaisNutricionaisResposta totais) {
}
