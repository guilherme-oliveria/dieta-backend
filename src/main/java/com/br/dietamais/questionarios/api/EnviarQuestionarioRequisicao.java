package com.br.dietamais.questionarios.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EnviarQuestionarioRequisicao(UUID id,@NotNull UUID modeloId) {}
