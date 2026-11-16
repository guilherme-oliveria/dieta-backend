package com.br.dietamais.dietas.api;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ModelosDietaResposta(@NotNull List<@NotNull @Valid ModeloDietaResposta> itens) {
}
