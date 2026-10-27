package com.br.dietamais.alimentos.api;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record AtualizarAlimentoRequisicao(
	@NotNull UUID grupoId,
	@NotBlank @Size(max=150) String nome,
	@NotBlank @Size(max=150) String medidaCaseira,
	@NotNull @Positive @Digits(integer=8,fraction=4) BigDecimal gramasPorPorcao,
	@NotNull Boolean ativo) {
}
