package com.br.dietamais.referencias.api;

import java.math.BigDecimal;

/** Atividade física com o seu equivalente metabólico (BE-44). {@code codigo} é estável e vai no contrato; {@code nome} é o texto da planilha. */
public record AtividadeMet(int ordem,String codigo,String nome,BigDecimal met,String fonte,String versaoReferencia){}
