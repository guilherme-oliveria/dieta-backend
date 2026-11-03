package com.br.dietamais.avaliacoes.api;

import java.math.BigDecimal;

/** Limites exclusivos quando diferentes; igualdade representa corte exato. Nulo é extremo aberto. */
public record IntervaloCurva(BigDecimal inferior,BigDecimal superior){}
