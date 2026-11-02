package com.br.dietamais.avaliacoes.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Resultados pediátricos congelados na escrita; leitura não recalcula por idade ou catálogo atual. */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ResultadosInfantisAvaliacao(int idadeMeses,String idadeTexto,String sexoReferencia,ModoCurva modoCurva,String classificacaoPesoNascer,
		BigDecimal pesoTeoricoMinKg,BigDecimal pesoTeoricoMaxKg,List<CurvaCrescimentoAvaliacao> curvas,List<FrisanchoAvaliacao> frisancho,
		ComposicaoInfantil composicao,EnergiaInfantil energia,Map<String,String> motivosNulos){}
