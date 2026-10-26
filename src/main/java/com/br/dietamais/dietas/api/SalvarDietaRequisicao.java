package com.br.dietamais.dietas.api;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record SalvarDietaRequisicao(
	UUID id,
	@NotBlank @Size(max=150) String titulo,
	@Size(max=10000) String orientacao,
	@NotNull List<@NotNull @Valid RefeicaoRequisicao> refeicoes) {
}
