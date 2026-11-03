package com.br.dietamais.comum.api;

import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** Uma mutação enviada pelo app em {@code POST /sync} (integracao.md). */
public record MutacaoSync(UUID idMutacao,String entidade,OperacaoSync operacao,UUID id,Long versaoBase,JsonNode dados) {}
