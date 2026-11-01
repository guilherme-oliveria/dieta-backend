package com.br.dietamais.dietas.api;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.*;

/** Exatamente uma origem: `alimentoId`, `grupoId` ou (sem os dois) `textoLivre` com `aVontade` (RN-DIE-10, RN-DIE-12). */
public record ItemRefeicaoRequisicao(
	UUID id,
	UUID alimentoId,
	/** Item por porção do grupo: usa a opção escolhida em `porcoes` da dieta. */
	UUID grupoId,
	/** Quantidade de porções; ausente só em item à vontade. */
	@Positive BigDecimal porcoes,
	/** Item sem quantidade e sem kcal (ex.: vegetal cru). */
	boolean aVontade,
	/** Alimento fora do catálogo, só como texto; não entra nos totais. */
	@Size(max=200) String textoLivre,
	@Size(max=2000) String observacao) {
	public ItemRefeicaoRequisicao(UUID id,UUID alimentoId,BigDecimal porcoes,String observacao){this(id,alimentoId,null,porcoes,false,null,observacao);}
}
