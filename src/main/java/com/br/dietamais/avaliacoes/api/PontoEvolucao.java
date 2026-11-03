package com.br.dietamais.avaliacoes.api;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record PontoEvolucao(@NotNull Instant dataHora,@NotNull BigDecimal valor) {}
