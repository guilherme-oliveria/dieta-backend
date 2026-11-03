package com.br.dietamais.relatorios.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** Estado do pedido de exportação. {@code url} só vem com {@code PRONTA} e vale poucos minutos; peça de novo para renovar. */
public record ExportacaoResposta(
	@NotNull UUID id,
	@NotBlank String status,
	@NotNull Instant criadoEm,
	Instant prontaEm,
	Instant expiraEm,
	String url) { }
