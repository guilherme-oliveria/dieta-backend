package com.br.dietamais.avaliacoes.api;

import java.math.BigDecimal;

/** Snapshot da medida e dos cortes de Frisancho, incluindo a fonte inconsistente quando houver. */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record FrisanchoAvaliacao(String indicador,BigDecimal valor,String unidade,Integer idadeAnosReferencia,
		BigDecimal p5,BigDecimal p10,BigDecimal p50,BigDecimal p90,BigDecimal adequacaoPercentual,String classificacao,
		IntervaloCurva percentil,String fonte,String versaoReferencia,String motivo){}
