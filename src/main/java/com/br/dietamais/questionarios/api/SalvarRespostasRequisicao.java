package com.br.dietamais.questionarios.api;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

/** Mescla por pergunta: o valor substitui a resposta anterior; {@code null} apaga a resposta. Perguntas não citadas ficam como estão. */
public record SalvarRespostasRequisicao(@NotNull Map<UUID,JsonNode> respostas) {}
