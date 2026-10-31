package com.br.dietamais.avaliacoes.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Corpo de criação e de substituição. {@code justificativa} só é exigida ao editar depois de 30 dias (RN-AVA-04). */
public record SalvarAvaliacaoRequisicao(
		UUID id,
		@NotNull Instant dataHora,
		@NotNull BigDecimal pesoKg,
		@NotNull BigDecimal estaturaM,
		@Valid PerimetrosAvaliacao perimetros,
		@Valid DobrasAvaliacao dobras,
		@Valid ComposicaoAvaliacao composicao,
		@Size(max=10000) String parecer,
		Boolean compartilhada,
		@Size(max=1000) String justificativa,
		@Valid ProtocoloAvaliacao protocolo,
		@Valid MedidasComplementaresAvaliacao complementares,
		@Valid CompartilhamentoAvaliacao compartilhamento) {
}
