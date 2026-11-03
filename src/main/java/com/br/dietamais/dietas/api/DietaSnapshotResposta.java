package com.br.dietamais.dietas.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record DietaSnapshotResposta(
	@NotNull UUID id,
	@NotNull UUID dietaId,
	@NotNull UUID pacienteId,
	@NotNull UUID vinculoId,
	@NotNull @Valid com.br.dietamais.pacientes.api.NutricionistaResumoResposta nutricionista,
	@NotBlank String titulo,
	String orientacao,
	@NotNull List<@NotNull @Valid RefeicaoResposta> refeicoes,
	@NotNull @Valid com.br.dietamais.alimentos.api.ListaSubstituicaoResposta listaSubstituicao,
	@NotNull @Valid TotaisNutricionaisResposta totais,
	@NotNull Instant publicadoEm) {
}
