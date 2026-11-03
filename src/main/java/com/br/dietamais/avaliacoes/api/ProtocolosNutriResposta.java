package com.br.dietamais.avaliacoes.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;

/** "Meus protocolos" (BE-42): padrões da nutri para as próximas avaliações; cada avaliação grava o que usou (RN-CAL-03). */
public record ProtocolosNutriResposta(@NotNull ProtocoloDobras dobras,@NotNull ProtocoloMassa massa,
		@NotNull @PositiveOrZero Long versao,@NotNull Instant atualizadoEm) {
}
