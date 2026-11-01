package com.br.dietamais.alimentos.api;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/** Alimento da lista da nutri com os macros por porção do grupo, para montar e calcular dietas. {@code marcadores} são as restrições que ele carrega (BE-51). */
public record AlimentoParaDieta(UUID id,UUID grupoId,String nome,String medidaCaseira,BigDecimal gramasPorPorcao,boolean ativo,
	BigDecimal kcalPorPorcao,BigDecimal carboidratosPorPorcao,BigDecimal proteinasPorPorcao,BigDecimal gordurasPorPorcao,String grupoNome,Set<MarcadorAlimento> marcadores) {
	public AlimentoParaDieta(UUID id,UUID grupoId,String nome,String medidaCaseira,BigDecimal gramasPorPorcao,boolean ativo,
		BigDecimal kcalPorPorcao,BigDecimal carboidratosPorPorcao,BigDecimal proteinasPorPorcao,BigDecimal gordurasPorPorcao,String grupoNome){
		this(id,grupoId,nome,medidaCaseira,gramasPorPorcao,ativo,kcalPorPorcao,carboidratosPorPorcao,proteinasPorPorcao,gordurasPorPorcao,grupoNome,Set.of());
	}
}
