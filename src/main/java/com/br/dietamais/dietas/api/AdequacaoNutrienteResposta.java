package com.br.dietamais.dietas.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Total do dia de um nutriente contra a meta DRI do paciente; `classe` = `ABAIXO` (< 90%), `ADEQUADO` (90% a 110%) ou `ACIMA` (> 110%).
 * `nome` e `unidade` (BE-67) são o rótulo e a unidade de `total` e `meta` (mg, mcg, g, l...), para o cliente não manter tabela própria.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdequacaoNutrienteResposta(String nutriente,BigDecimal total,BigDecimal meta,BigDecimal percentual,String classe,
		@Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) String nome,
		@Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) String unidade) {
	public AdequacaoNutrienteResposta(String nutriente,BigDecimal total,BigDecimal meta,BigDecimal percentual,String classe){this(nutriente,total,meta,percentual,classe,null,null);}
}
