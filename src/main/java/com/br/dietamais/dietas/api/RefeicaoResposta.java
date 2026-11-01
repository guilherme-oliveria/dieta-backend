package com.br.dietamais.dietas.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record RefeicaoResposta(
	@NotNull UUID id,
	@NotBlank String nome,
	@NotNull LocalTime horario,
	@PositiveOrZero int ordem,
	@NotNull List<@NotNull @Valid ItemRefeicaoResposta> itens,
	@NotNull @Valid TotaisNutricionaisResposta totais,
	/** `PRINCIPAL` ou `ALTERNATIVA` (RN-DIE-11); snapshots antigos não têm e valem `PRINCIPAL`. */
	@Schema(requiredMode=Schema.RequiredMode.NOT_REQUIRED) String tipo,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) UUID alternativaDe,
	/** Só na alternativa: total dela menos o da refeição que substitui. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Valid @Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) DiferencaTotaisResposta diferencaParaPrincipal) {
	public RefeicaoResposta(UUID id,String nome,LocalTime horario,int ordem,List<ItemRefeicaoResposta> itens,TotaisNutricionaisResposta totais){this(id,nome,horario,ordem,itens,totais,"PRINCIPAL",null,null);}
	public boolean alternativa(){return "ALTERNATIVA".equals(tipo);}
}
