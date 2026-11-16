package com.br.dietamais.alimentos.api;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Produto da nutri com os nutrientes por 100 g (ou 100 mL) para entrar como item da dieta (BE-48c, RN-ALI-05).
 * {@code composicao} usa os códigos de {@code GET /referencias/porcoes}; nutriente que o produto não informa fica de fora (a dieta marca {@code incompleto}).
 */
public record ProdutoParaDieta(UUID id,String nome,String medidaCaseira,boolean ativo,Map<String,BigDecimal> composicao) {
	/** Chave do produto (BE-48a) para o código do nutriente da dieta; as unidades coincidem. */
	private static final Map<String,String> CODIGOS=Map.ofEntries(Map.entry("energia_kcal","energia"),Map.entry("carboidrato_g","gli"),Map.entry("proteina_g","pro"),Map.entry("lipideos_g","lip"),
		Map.entry("saturados_g","sat"),Map.entry("monoinsaturados_g","mon"),Map.entry("poliinsaturados_g","pol"),Map.entry("trans_g","trans"),Map.entry("colesterol_mg","col"),
		Map.entry("fibra_g","fib"),Map.entry("sodio_mg","na"),Map.entry("potassio_mg","k"),Map.entry("calcio_mg","ca"),Map.entry("cobre_ug","cu"),Map.entry("iodo_ug","i"),
		Map.entry("magnesio_mg","mg"),Map.entry("manganes_mg","mn"),Map.entry("fosforo_mg","p"),Map.entry("selenio_ug","se"),Map.entry("zinco_mg","zn"),Map.entry("ferro_mg","fe"),
		Map.entry("acido_folico_ug","fol"),Map.entry("niacina_mg","nia"),Map.entry("acido_pantotenico_mg","pan"),Map.entry("riboflavina_mg","b2"),Map.entry("tiamina_mg","b1"),
		Map.entry("vitamina_a_ug","vita"),Map.entry("piridoxina_mg","b6"),Map.entry("vitamina_b12_ug","b12"),Map.entry("vitamina_c_mg","c"),Map.entry("vitamina_d_ug","d"),Map.entry("vitamina_e_mg","e"));
	public static Map<String,BigDecimal> paraCodigos(Map<String,BigDecimal> nutrientes){
		var r=new LinkedHashMap<String,BigDecimal>();
		nutrientes.forEach((k,v)->{var c=CODIGOS.get(k);if(c!=null&&v!=null)r.put(c,v);});
		return r;
	}
}
