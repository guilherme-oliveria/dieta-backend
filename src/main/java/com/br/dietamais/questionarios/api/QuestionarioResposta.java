package com.br.dietamais.questionarios.api;

import tools.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** {@code blocos} são as perguntas congeladas no envio (RN-ANA-04). {@code respostas} mapeia id da pergunta para o valor (formato por tipo em {@link TipoPergunta}). */
public record QuestionarioResposta(@NotNull UUID id,@NotNull UUID pacienteId,@NotNull UUID vinculoId,UUID modeloId,@NotNull String nome,
		@NotNull List<@NotNull @Valid BlocoQuestionario> blocos,@NotNull Map<UUID,JsonNode> respostas,@NotNull StatusQuestionario status,
		@NotNull Instant enviadoEm,Instant respondidoEm,@NotNull @PositiveOrZero Long versao,@NotNull Instant criadoEm,@NotNull Instant atualizadoEm,
		@io.swagger.v3.oas.annotations.media.Schema(description="Resultados calculados somente pelo servidor; cada resultado é nulo quando faltam entradas. Congelados na conclusão.",accessMode=io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY)
		AnamneseDerivada derivados) {}
