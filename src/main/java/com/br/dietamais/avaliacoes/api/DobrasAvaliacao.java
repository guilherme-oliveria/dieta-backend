package com.br.dietamais.avaliacoes.api;

import java.math.BigDecimal;

/** Dobras cutâneas em mm (RN-AVA-01/03: 1 a 80). Todas opcionais. */
public record DobrasAvaliacao(BigDecimal triceps,BigDecimal subescapular,BigDecimal peitoral,BigDecimal axilarMedia,
		BigDecimal suprailiaca,BigDecimal abdominal,BigDecimal coxa) {
	public static final DobrasAvaliacao VAZIO=new DobrasAvaliacao(null,null,null,null,null,null,null);
}
