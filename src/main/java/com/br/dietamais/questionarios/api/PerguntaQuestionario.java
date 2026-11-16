package com.br.dietamais.questionarios.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** {@code obrigatoria} ausente vale falso. {@code id} é UUIDv7; ao criar/substituir um modelo, ausente = o backend gera. As respostas e as condições referenciam este id. */
public record PerguntaQuestionario(UUID id,@NotBlank @Size(max=500) String texto,@NotNull TipoPergunta tipo,
		@Size(max=30) List<@NotBlank @Size(max=100) String> opcoes,Boolean obrigatoria,@Valid CondicaoPergunta condicao,
		@io.swagger.v3.oas.annotations.media.Schema(description="Código opcional estável da anamnese. Único no modelo; define tipo/unidade dos cálculos, independentemente do texto.")
		@Size(max=80) @jakarta.validation.constraints.Pattern(regexp="[A-Z][A-Z0-9_]*") String codigo) {
	public PerguntaQuestionario(UUID id,String texto,TipoPergunta tipo,List<String> opcoes,Boolean obrigatoria,CondicaoPergunta condicao){
		this(id,texto,tipo,opcoes,obrigatoria,condicao,null);
	}
}
