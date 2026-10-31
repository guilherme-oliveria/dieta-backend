package com.br.dietamais.avaliacoes.api;

import jakarta.validation.constraints.NotNull;

/** Corpo de {@code PUT /eu/nutri/protocolos} (com {@code If-Match}). Protocolo desconhecido: 422 {@code PROTOCOLO_INVALIDO}. */
public record SalvarProtocolosNutriRequisicao(@NotNull ProtocoloDobras dobras,@NotNull ProtocoloMassa massa) {
}
