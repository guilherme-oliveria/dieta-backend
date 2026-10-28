package com.br.dietamais.relatorios.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** Link temporário para baixar o PDF gerado; o arquivo some em seguida (RN-LGPD: nada de dado de saúde guardado). */
public record PdfResposta(@NotBlank String url,@NotNull Instant expiraEm) { }
