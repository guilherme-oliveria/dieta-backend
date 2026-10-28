package com.br.dietamais.questionarios.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Condição de exibição (RN-ANA-02): a pergunta só aparece quando a resposta da pergunta anterior {@code perguntaId} satisfaz o operador. */
public record CondicaoPergunta(@NotNull UUID perguntaId,@NotNull OperadorCondicao operador,@NotBlank @Size(max=100) String valor) {}
