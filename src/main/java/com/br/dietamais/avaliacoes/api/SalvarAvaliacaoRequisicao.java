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
		@Valid CompartilhamentoAvaliacao compartilhamento,
		@Valid EnergiaAvaliacao energia) {
	/** Contrato anterior ao BE-43b, sem as escolhas de energia. */
	public SalvarAvaliacaoRequisicao(UUID id,Instant dataHora,BigDecimal pesoKg,BigDecimal estaturaM,PerimetrosAvaliacao perimetros,DobrasAvaliacao dobras,ComposicaoAvaliacao composicao,
			String parecer,Boolean compartilhada,String justificativa,ProtocoloAvaliacao protocolo,MedidasComplementaresAvaliacao complementares,CompartilhamentoAvaliacao compartilhamento){
		this(id,dataHora,pesoKg,estaturaM,perimetros,dobras,composicao,parecer,compartilhada,justificativa,protocolo,complementares,compartilhamento,null);
	}
}
