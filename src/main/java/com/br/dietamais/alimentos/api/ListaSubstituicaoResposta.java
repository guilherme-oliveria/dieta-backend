package com.br.dietamais.alimentos.api;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record ListaSubstituicaoResposta(
	@NotNull UUID pacienteId,
	@NotNull @PositiveOrZero Long versao,
	@NotNull List<@NotNull @Valid AlimentoResposta> alimentos,
	@NotNull List<@NotNull UUID> adicionados,
	@NotNull List<@NotNull UUID> removidos) {
}
