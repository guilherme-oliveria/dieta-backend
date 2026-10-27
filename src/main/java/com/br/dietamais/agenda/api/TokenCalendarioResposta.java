package com.br.dietamais.agenda.api;

import jakarta.validation.constraints.NotBlank;

/** O token aparece só aqui; o app guarda o link. Criar outro revoga o anterior. */
public record TokenCalendarioResposta(@NotBlank String token,@NotBlank String url,@NotBlank String urlWebcal) {}
