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
	@NotNull @Valid TotaisNutricionaisResposta totais,
	/** Nome do grupo (campo opcional: snapshots antigos não têm; serve para agrupar a lista de compras). */
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	@io.swagger.v3.oas.annotations.media.Schema(nullable=true,requiredMode=io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED)
	String grupoNome) {
}
