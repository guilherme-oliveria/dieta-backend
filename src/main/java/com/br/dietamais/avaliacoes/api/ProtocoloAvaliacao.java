package com.br.dietamais.avaliacoes.api;

import jakarta.validation.constraints.NotNull;

/**
 * Protocolos usados na avaliação (RN-CAL-03). Na resposta, {@code versaoReferencia} identifica as tabelas de
 * referência usadas no cálculo; na requisição o servidor ignora o valor e grava o vigente.
 */
public record ProtocoloAvaliacao(@NotNull ProtocoloDobras dobras,@NotNull ProtocoloMassa massa,String versaoReferencia) {
}
