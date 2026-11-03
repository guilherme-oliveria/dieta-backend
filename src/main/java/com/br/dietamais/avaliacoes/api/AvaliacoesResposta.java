package com.br.dietamais.avaliacoes.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AvaliacoesResposta(@NotNull List<@NotNull @Valid AvaliacaoResposta> itens) {
}
