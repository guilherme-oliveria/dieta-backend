package com.br.dietamais.referencias.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/** Faixa numérica de um exame já resolvida para o paciente (sexo, idade e fase), para desenhar a régua (BE-56c). Limite ausente = aberto. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FaixaExame(String rotulo,BigDecimal min,boolean minIncl,BigDecimal max,boolean maxIncl){}
