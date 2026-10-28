package com.br.dietamais.assinaturas.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PlanoResposta(@NotBlank String codigo,@NotBlank String nome,@NotNull @PositiveOrZero BigDecimal precoMensal,@NotNull @PositiveOrZero Integer limitePacientes) { }
