package com.br.dietamais.assinaturas.api;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

/** Visão do admin: conta e plano, sem nenhum dado de paciente (RN-LGPD-05); {@code pacientesOcupados} é só uma contagem. */
public record AssinaturaAdminResposta(
	@NotNull UUID id,
	@NotNull UUID organizacaoId,
	@NotBlank String organizacaoNome,
	@NotBlank String planoCodigo,
	@NotNull StatusAssinatura status,
	@NotNull Instant inicioEm,
	Instant fimPeriodoEm,
	@NotNull @PositiveOrZero Integer pacientesOcupados,
	@NotNull Boolean gatewayVinculado) { }
