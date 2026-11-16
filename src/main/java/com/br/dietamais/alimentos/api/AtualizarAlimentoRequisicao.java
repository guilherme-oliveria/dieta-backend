package com.br.dietamais.alimentos.api;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record AtualizarAlimentoRequisicao(
	@NotNull UUID grupoId,
	@NotBlank @Size(max=150) String nome,
	@NotBlank @Size(max=150) String medidaCaseira,
	@NotNull @Positive @Digits(integer=8,fraction=4) BigDecimal gramasPorPorcao,
	@NotNull Boolean ativo,
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) java.util.List<MarcadorAlimento> marcadores) {
	/** {@code marcadores} ausente mantém os atuais; lista vazia limpa (BE-51). */
	public AtualizarAlimentoRequisicao(UUID grupoId,String nome,String medidaCaseira,BigDecimal gramasPorPorcao,Boolean ativo){this(grupoId,nome,medidaCaseira,gramasPorPorcao,ativo,null);}
}
