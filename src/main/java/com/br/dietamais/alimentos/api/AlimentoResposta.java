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
	@NotNull Instant atualizadoEm,
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) java.util.List<MarcadorAlimento> marcadores) {
	/** Contrato anterior ao BE-51, sem marcadores. */
	public AlimentoResposta(UUID id,UUID catalogoId,UUID grupoId,String nome,String medidaCaseira,BigDecimal gramasPorPorcao,boolean ativo,Long versao,Instant atualizadoEm){
		this(id,catalogoId,grupoId,nome,medidaCaseira,gramasPorPorcao,ativo,versao,atualizadoEm,null);
	}
}
