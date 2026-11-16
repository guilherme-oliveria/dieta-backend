package com.br.dietamais.dietas.api;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.*;

/** Exatamente uma origem: `alimentoId`, `grupoId`, `produtoId` ou (sem os três) `textoLivre` com `aVontade` (RN-DIE-10, RN-DIE-12). */
public record ItemRefeicaoRequisicao(
	UUID id,
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	UUID alimentoId,
	/** Item por porção do grupo: usa a opção escolhida em `porcoes` da dieta. */
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	UUID grupoId,
	/** Quantidade de porções; ausente só em item à vontade. */
	@Positive BigDecimal porcoes,
	/** Item sem quantidade e sem kcal (ex.: vegetal cru). */
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	Boolean aVontade,
	/** Alimento fora do catálogo, só como texto; não entra nos totais. */
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	@Size(max=200) String textoLivre,
	@Size(max=2000) String observacao,
	/** Produto da nutri (BE-48c): exige `gramas` e não aceita `porcoes`. */
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	UUID produtoId,
	/** Quantidade em gramas (ou mL) do item por produto. */
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	@Positive BigDecimal gramas) {
	public ItemRefeicaoRequisicao(UUID id,UUID alimentoId,BigDecimal porcoes,String observacao){this(id,alimentoId,null,porcoes,null,null,observacao,null,null);}
	public ItemRefeicaoRequisicao(UUID id,UUID alimentoId,UUID grupoId,BigDecimal porcoes,Boolean aVontade,String textoLivre,String observacao){this(id,alimentoId,grupoId,porcoes,aVontade,textoLivre,observacao,null,null);}
}
