package com.br.dietamais.avaliacoes.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Atividade física do dia para o GET por MET; {@code codigo} vem de {@code GET /referencias/atividades-met}. */
public record AtividadeDiaria(@NotBlank @Size(max=80) String codigo,@NotNull BigDecimal minutos) {
}
