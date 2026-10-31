package com.br.dietamais.avaliacoes.api;

import java.math.BigDecimal;

/**
 * Campos novos da avaliação ampliada (BE-C7, gravados a partir do BE-41), todos opcionais. Limites de sanidade
 * (RN-AVA-03): pesos 20–400 kg, punho 5–40 cm, diâmetros 1–30 cm, % desejado 3–60.
 */
public record MedidasComplementaresAvaliacao(BigDecimal pesoUsualKg,BigDecimal pesoDesejadoKg,BigDecimal punhoCm,
		BigDecimal diametroPunhoCm,BigDecimal diametroJoelhoCm,BigDecimal percentualGorduraDesejado,
		NivelAtividade nivelAtividade,ObjetivoPeso objetivo,BigDecimal objetivoKgPorMes,Boolean menstruada) {
}
