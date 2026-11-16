package com.br.dietamais.avaliacoes.api;

import java.math.BigDecimal;
import java.util.Map;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnergiaInfantil(BigDecimal gebIomKcal,BigDecimal getIomKcal,BigDecimal getRdiKcal,Map<String,String> motivosNulos){}
