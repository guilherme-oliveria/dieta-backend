package com.br.dietamais.avaliacoes.api;

import java.math.BigDecimal;
import java.util.List;

/** Snapshot da medida, cortes e posição: não representa percentil ou escore interpolado. */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record CurvaCrescimentoAvaliacao(String indicador,BigDecimal valor,String unidade,Integer idadeMesesReferencia,
		ModoCurva modo,String classificacao,IntervaloCurva percentil,IntervaloCurva escoreZ,List<BigDecimal> cortesPercentis,
		List<BigDecimal> cortesEscoresZ,String fonte,String versaoReferencia,String motivo){}
