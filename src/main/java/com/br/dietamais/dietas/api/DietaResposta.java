package com.br.dietamais.dietas.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record DietaResposta(
	@NotNull UUID id,
	@NotNull UUID pacienteId,
	@NotNull UUID vinculoId,
	@NotBlank String titulo,
	@NotNull StatusDieta status,
	String orientacao,
	@NotNull List<@NotNull @Valid RefeicaoResposta> refeicoes,
	@NotNull @Valid TotaisNutricionaisResposta totais,
	@NotNull @PositiveOrZero Long versao,
	@NotNull Instant criadoEm,
	@NotNull Instant atualizadoEm) {
}
