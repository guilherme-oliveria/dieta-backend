package com.br.dietamais.agenda.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ConsultasResposta(@NotNull List<@NotNull @Valid ConsultaResposta> itens) {
}
