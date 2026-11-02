package com.br.dietamais.questionarios.api;

import java.math.BigDecimal;
import java.util.Map;

/** Resultados do servidor; nulo significa entrada insuficiente, nunca consumo zero. */
public record AnamneseDerivada(String pressaoArterial,Alcool alcool,Integer salGramasPorPessoaDia,
		Integer oleoMlPorPessoaDia,Integer oleoKcalPorPessoaDia,Integer atividadeDuracaoMinutos,Map<String,Integer> atividadesDuracaoMinutos) {
	public record Alcool(BigDecimal mlPorSemana,BigDecimal gramasEtanolPorSemana,BigDecimal kcalPorSemana) {}
}
