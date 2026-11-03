package com.br.dietamais.assinaturas.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlanoAdminResposta(
	@NotNull UUID id,
	@NotBlank String codigo,
	@NotBlank String nome,
	@NotNull @PositiveOrZero BigDecimal precoMensal,
	@NotNull @Positive Integer limitePacientes,
	Integer duracaoDias,
	@NotNull Boolean ativo,
	@NotNull Instant criadoEm) { }
