package com.br.dietamais.avaliacoes.api;

import java.math.BigDecimal;
import java.util.Map;

/** Gordura por Slaughter, classificada por Deurenberg; somente de 10 a 17 anos. */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ComposicaoInfantil(BigDecimal percentualGordura,String classificacaoPercentualGordura,BigDecimal percentualMagro,
		BigDecimal pesoGordoKg,BigDecimal pesoMagroKg,Map<String,String> motivosNulos){}
