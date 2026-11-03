package com.br.dietamais.alimentos.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record AlimentoResposta(
	@NotNull UUID id,
	UUID catalogoId,
	@NotNull UUID grupoId,
	@NotBlank String nome,
	@NotBlank String medidaCaseira,
	@NotNull @Positive BigDecimal gramasPorPorcao,
	boolean ativo,
	@NotNull @PositiveOrZero Long versao,
	@NotNull Instant atualizadoEm) {
}
