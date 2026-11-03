package com.br.dietamais.dietas.api;

import com.br.dietamais.alimentos.api.MarcadorAlimento;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.*;

/** Salva uma dieta existente como modelo da nutri (BE-52); o conteúdo vem da dieta. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SalvarComoModeloRequisicao(
	UUID id,
	@NotBlank @Size(max=150) String titulo,
	@Size(max=500) String descricao,
	@Size(max=60) String condicao,
	@Size(max=5) List<@NotNull MarcadorAlimento> restricoes) {
}
