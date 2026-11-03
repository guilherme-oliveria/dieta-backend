package com.br.dietamais.alimentos.api;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record AtualizarListaSubstituicaoRequisicao(
	@NotNull List<@NotNull UUID> adicionados,
	@NotNull List<@NotNull UUID> removidos) {
}
