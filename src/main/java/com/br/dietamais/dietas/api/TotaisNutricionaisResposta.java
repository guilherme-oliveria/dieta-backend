package com.br.dietamais.dietas.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record TotaisNutricionaisResposta(
	@NotNull @PositiveOrZero BigDecimal kcal,
	@NotNull @PositiveOrZero BigDecimal carboidratos,
	@NotNull @PositiveOrZero BigDecimal proteinas,
	@NotNull @PositiveOrZero BigDecimal gorduras,
	/** Só no total do dia e só quando a dieta tem metas (RN-DIE-10). */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Valid @Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) SaldoMetasResposta saldoMetas,
	/** Código do nutriente (os de `GET /referencias/porcoes`, menos energia, carboidrato, proteína e lipídio) → total; só itens que informam o nutriente entram na soma (RN-DIE-13). Ausente quando nenhum item informa micronutrientes. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) Map<String,BigDecimal> micronutrientes,
	/** Códigos de `micronutrientes` em que algum item da soma não informa o nutriente: o total é parcial, nunca "zero por falta de dado". */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) List<String> incompleto,
	/** Só no total do dia, quando a ficha tem sexo e nascimento. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Valid @Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) List<@NotNull @Valid AdequacaoNutrienteResposta> adequacao) {
	public TotaisNutricionaisResposta(BigDecimal kcal,BigDecimal carboidratos,BigDecimal proteinas,BigDecimal gorduras){this(kcal,carboidratos,proteinas,gorduras,null,null,null,null);}
	public TotaisNutricionaisResposta comSaldo(SaldoMetasResposta saldo){return new TotaisNutricionaisResposta(kcal,carboidratos,proteinas,gorduras,saldo,micronutrientes,incompleto,adequacao);}
	public TotaisNutricionaisResposta comMicronutrientes(Map<String,BigDecimal> micros,List<String> faltantes){return new TotaisNutricionaisResposta(kcal,carboidratos,proteinas,gorduras,saldoMetas,micros,faltantes,adequacao);}
	public TotaisNutricionaisResposta comAdequacao(List<AdequacaoNutrienteResposta> lista){return new TotaisNutricionaisResposta(kcal,carboidratos,proteinas,gorduras,saldoMetas,micronutrientes,incompleto,lista);}
}
