package com.br.dietamais.questionarios.api;

import java.util.UUID;

/** Evento de domínio: a nutri enviou um questionário ao paciente (RN-NOT-01; consumido pelo push na BE-24). Sem conteúdo clínico. */
public record QuestionarioEnviado(UUID questionarioId,UUID nutricionistaId,UUID pacienteId) {}
