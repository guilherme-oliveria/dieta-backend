package com.br.dietamais.alimentos.api;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record CriarAlimentoRequisicao(
	UUID id,
	@NotNull UUID grupoId,
	@NotBlank @Size(max=150) String nome,
	@NotBlank @Size(max=150) String medidaCaseira,
	@NotNull @Positive @Digits(integer=8,fraction=4) BigDecimal gramasPorPorcao,
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) java.util.List<MarcadorAlimento> marcadores) {
	public CriarAlimentoRequisicao(UUID id,UUID grupoId,String nome,String medidaCaseira,BigDecimal gramasPorPorcao){this(id,grupoId,nome,medidaCaseira,gramasPorPorcao,null);}
}
