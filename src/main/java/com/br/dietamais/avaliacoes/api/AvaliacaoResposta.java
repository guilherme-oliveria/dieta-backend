package com.br.dietamais.avaliacoes.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AvaliacaoResposta(
		@NotNull UUID id,
		@NotNull UUID pacienteId,
		@NotNull UUID vinculoId,
		@NotNull Instant dataHora,
		@NotNull BigDecimal pesoKg,
		@NotNull BigDecimal estaturaM,
		@NotNull BigDecimal imc,
		@NotNull ClassificacaoImc classificacaoImc,
		@NotNull @Valid PerimetrosAvaliacao perimetros,
		@NotNull @Valid DobrasAvaliacao dobras,
		@NotNull @Valid ComposicaoAvaliacao composicao,
		String parecer,
		@NotNull Boolean compartilhada,
		@NotNull @PositiveOrZero Long versao,
		@NotNull Instant criadoEm,
		@NotNull Instant atualizadoEm) {
}
