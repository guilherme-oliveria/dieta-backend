package com.br.dietamais.sync.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record EnviarMutacoesRequisicao(@NotNull @Size(min = 1, max = 200) List<@NotNull @Valid MutacaoSyncRequisicao> mutacoes) {
}
