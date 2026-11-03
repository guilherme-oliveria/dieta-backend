package com.br.dietamais.questionarios.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record QuestionariosResposta(@NotNull List<@NotNull @Valid QuestionarioResposta> itens) {}
