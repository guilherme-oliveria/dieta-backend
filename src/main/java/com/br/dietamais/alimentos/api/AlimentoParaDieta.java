package com.br.dietamais.alimentos.api;

import java.math.BigDecimal;
import java.util.UUID;

/** Alimento da lista da nutri com os macros por porção do grupo, para montar e calcular dietas. */
public record AlimentoParaDieta(UUID id,UUID grupoId,String nome,String medidaCaseira,BigDecimal gramasPorPorcao,boolean ativo,
	BigDecimal kcalPorPorcao,BigDecimal carboidratosPorPorcao,BigDecimal proteinasPorPorcao,BigDecimal gordurasPorPorcao,String grupoNome) {}
