package com.br.dietamais.avaliacoes.api;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Gasto energético e metas calculados e gravados pelo servidor (RN-CAL-01), com o método que foi usado. kcal, g e % com 2 casas.
 * Resultado sem dado suficiente fica ausente e o motivo vai em {@code motivosNulos} (RN-CAL-04), por exemplo
 * {@code "get": "FALTA_NIVEL_ATIVIDADE"}. {@code gebKcal} não existe no método RDI 2005.
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnergiaResultado(MetodoGet metodo,BigDecimal gebKcal,BigDecimal getKcal,BigDecimal vetKcal,BigDecimal kcalPorKg,MetaMacro carboidrato,
		MetaMacro proteina,MetaMacro lipidio,LipidiosDislipidemico lipidiosDislipidemico,BigDecimal colesterolMaximoMg,Map<String,String> motivosNulos) {
}
