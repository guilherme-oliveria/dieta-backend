package com.br.dietamais.avaliacoes.api;

import java.math.BigDecimal;

/** Resultados de composição corporal digitados pela nutri (RN-AVA-02): o backend não os calcula no MVP. */
public record ComposicaoAvaliacao(BigDecimal percentualGordura,BigDecimal pesoGordoKg,BigDecimal pesoMagroKg) {
	public static final ComposicaoAvaliacao VAZIO=new ComposicaoAvaliacao(null,null,null);
}
