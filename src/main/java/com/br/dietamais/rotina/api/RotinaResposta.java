package com.br.dietamais.rotina.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Registros do próprio paciente num período, no mesmo formato do /sync (BE-38). Ordenados do mais antigo para o mais novo. */
public record RotinaResposta(
	@NotNull List<@NotNull @Valid CheckinResposta> checkins,
	@NotNull List<@NotNull @Valid AguaResposta> agua,
	@NotNull List<@NotNull @Valid PesoResposta> pesos) {
}
