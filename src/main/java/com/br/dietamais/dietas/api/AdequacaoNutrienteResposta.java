package com.br.dietamais.dietas.api;

import java.math.BigDecimal;

/** Total do dia de um nutriente contra a meta DRI do paciente; `classe` = `ABAIXO` (< 90%), `ADEQUADO` (90% a 110%) ou `ACIMA` (> 110%). */
public record AdequacaoNutrienteResposta(String nutriente,BigDecimal total,BigDecimal meta,BigDecimal percentual,String classe) {
}
