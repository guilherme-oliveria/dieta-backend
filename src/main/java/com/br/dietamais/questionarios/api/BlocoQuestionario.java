package com.br.dietamais.questionarios.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BlocoQuestionario(@NotBlank @Size(max=120) String titulo,@NotEmpty @Size(max=60) List<@Valid PerguntaQuestionario> perguntas) {}
