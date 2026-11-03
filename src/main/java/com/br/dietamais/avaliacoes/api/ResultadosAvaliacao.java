package com.br.dietamais.avaliacoes.api;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resultados derivados, calculados e gravados pelo servidor (RN-CAL-01). Resultado sem dado suficiente fica ausente e o
 * motivo vem em {@code motivosNulos} (RN-CAL-04), por exemplo {@code "percentualGordura": "FALTA_DOBRAS"}; nunca zero.
 * Kg e % com 2 casas, densidade com 4.
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ResultadosAvaliacao(
		Integer idadeAnos,
		ClasseImc classificacaoImc,
		RiscoComorbidades riscoComorbidades,
		BigDecimal pesoTeoricoMinKg,
		BigDecimal pesoTeoricoMaxKg,
		Compleicao compleicao,
		ClasseCintura classificacaoCintura,
		BigDecimal somatorioDobrasMm,
		BigDecimal densidadeCorporal,
		BigDecimal percentualGordura,
		ClassePercentualGordura classificacaoPercentualGordura,
		BigDecimal percentualMagro,
		BigDecimal pesoGordoKg,
		BigDecimal pesoOsseoKg,
		BigDecimal pesoResidualKg,
		BigDecimal pesoMagroKg,
		BigDecimal pesoParaPercentualDesejadoKg,
		String textoResumo,
		Map<String,String> motivosNulos) {
}
