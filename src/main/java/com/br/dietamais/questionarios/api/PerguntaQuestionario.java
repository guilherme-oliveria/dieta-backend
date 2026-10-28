package com.br.dietamais.questionarios.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** {@code obrigatoria} ausente vale falso. {@code id} é UUIDv7; ao criar/substituir um modelo, ausente = o backend gera. As respostas e as condições referenciam este id. */
public record PerguntaQuestionario(UUID id,@NotBlank @Size(max=500) String texto,@NotNull TipoPergunta tipo,
		@Size(max=30) List<@NotBlank @Size(max=100) String> opcoes,Boolean obrigatoria,@Valid CondicaoPergunta condicao) {}
