package com.br.dietamais.rotina.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Registros do paciente num período (BE-38), no mesmo formato do /sync; a web, sem banco local, lê o "Hoje" por aqui. */
public record RotinaResposta(
	@NotNull List<@NotNull @Valid CheckinResposta> checkins,
	@NotNull List<@NotNull @Valid AguaResposta> agua,
	@NotNull List<@NotNull @Valid PesoResposta> pesos) {
}
