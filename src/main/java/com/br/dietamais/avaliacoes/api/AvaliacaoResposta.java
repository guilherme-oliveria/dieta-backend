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
		@NotNull Instant atualizadoEm,
		@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) FaixaEtaria faixaEtaria,
		@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) @Valid ProtocoloAvaliacao protocolo,
		@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) @Valid MedidasComplementaresAvaliacao complementares,
		@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) @Valid ResultadosAvaliacao resultados,
		@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) @Valid CompartilhamentoAvaliacao compartilhamento,
		@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) @Valid EnergiaAvaliacao energia,
		@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) @Valid EnergiaResultado energiaResultado) {
	/** Contrato anterior ao BE-43b, sem gasto energético. */
	public AvaliacaoResposta(UUID id,UUID pacienteId,UUID vinculoId,Instant dataHora,BigDecimal pesoKg,BigDecimal estaturaM,BigDecimal imc,ClassificacaoImc classificacaoImc,PerimetrosAvaliacao perimetros,
			DobrasAvaliacao dobras,ComposicaoAvaliacao composicao,String parecer,Boolean compartilhada,Long versao,Instant criadoEm,Instant atualizadoEm,FaixaEtaria faixaEtaria,ProtocoloAvaliacao protocolo,
			MedidasComplementaresAvaliacao complementares,ResultadosAvaliacao resultados,CompartilhamentoAvaliacao compartilhamento){
		this(id,pacienteId,vinculoId,dataHora,pesoKg,estaturaM,imc,classificacaoImc,perimetros,dobras,composicao,parecer,compartilhada,versao,criadoEm,atualizadoEm,faixaEtaria,protocolo,complementares,resultados,compartilhamento,null,null);
	}
}
