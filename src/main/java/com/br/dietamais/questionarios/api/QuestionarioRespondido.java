package com.br.dietamais.questionarios.api;

import java.util.UUID;

/** Evento de domínio: o paciente concluiu o questionário (RN-ANA-03). Sem conteúdo clínico. */
public record QuestionarioRespondido(UUID questionarioId,UUID nutricionistaId,UUID pacienteId) {}
