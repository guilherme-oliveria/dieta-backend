package com.br.dietamais.assinaturas.api;

import jakarta.validation.constraints.NotBlank;

/** URL do checkout hospedado do gateway; o app abre no navegador externo e nunca vende dentro dele. */
public record CheckoutResposta(@NotBlank String url) { }
