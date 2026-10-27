package com.br.dietamais.sync.api;

import com.br.dietamais.comum.api.ResultadoMutacao;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record EnviarMutacoesResposta(@NotNull List<ResultadoMutacao> resultados) {
}
