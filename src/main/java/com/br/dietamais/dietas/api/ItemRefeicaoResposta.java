package com.br.dietamais.dietas.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Item da refeição. Item por alimento traz `alimentoId`; por grupo traz `grupoId` e `opcao`, e `porcoes` é a quantidade da porção do grupo;
 * à vontade e texto livre não têm quantidade (campos nulos) e totais zero (RN-DIE-10, RN-DIE-12).
 */
public record ItemRefeicaoResposta(
	@NotNull UUID id,
	@Schema(nullable=true) UUID alimentoId,
	@Schema(nullable=true) UUID grupoId,
	/** Nome do alimento; no item por grupo, o nome do grupo; no texto livre, o próprio texto. */
	@Schema(nullable=true) String nomeAlimento,
	@Schema(nullable=true) @Positive BigDecimal porcoes,
	@Schema(nullable=true) String medidaCaseira,
	@Schema(nullable=true) @Positive BigDecimal gramas,
	String observacao,
	@NotNull @Valid TotaisNutricionaisResposta totais,
	/** Nome do grupo (campo opcional: snapshots antigos não têm; serve para agrupar a lista de compras). */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED)
	String grupoNome,
	/** Opção da porção do grupo usada (só no item por grupo). */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) Integer opcao,
	@Schema(requiredMode=Schema.RequiredMode.NOT_REQUIRED) boolean aVontade,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) String textoLivre) {
	public ItemRefeicaoResposta(UUID id,UUID alimentoId,UUID grupoId,String nomeAlimento,BigDecimal porcoes,String medidaCaseira,BigDecimal gramas,String observacao,TotaisNutricionaisResposta totais,String grupoNome){
		this(id,alimentoId,grupoId,nomeAlimento,porcoes,medidaCaseira,gramas,observacao,totais,grupoNome,null,false,null);
	}
}
