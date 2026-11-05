package com.br.dietamais.alimentos.dominio;

import java.util.Set;

/** Chaves aceitas em {@code nutrientes} de um produto (por 100 g ou 100 mL): as mesmas do catálogo base (BE-48a); o mínimo obrigatório alimenta os totais da dieta (BE-48c). */
public final class NutrientesProduto {
	private NutrientesProduto(){}
	public static final Set<String> MINIMOS=Set.of("energia_kcal","carboidrato_g","proteina_g","lipideos_g");
	public static final Set<String> CHAVES=Set.of(
		"energia_kcal","densidade_calorica_kcal_ml","carboidrato_g","polioes_g","amido_g","sacarose_g"
		,"acucares_g","lactose_g","frutose_g","glicose_g","maltose_g","outros_glicideos_g"
		,"proteina_g","proteina_soja_g","outras_proteinas_g","arginina_g","taurina_mg","l_carnitina_mg"
		,"lipideos_g","saturados_g","monoinsaturados_g","poliinsaturados_g","trans_g","alfa_linoleico_g"
		,"alfa_linolenico_g","araquidonico_mg","docosahexaenoico_mg","tcm_g","colesterol_mg","fibra_g"
		,"fibra_soluvel_g","fibra_insoluvel_g","sodio_mg","potassio_mg","calcio_mg","fosforo_mg"
		,"ferro_mg","cobre_ug","iodo_ug","magnesio_mg","manganes_mg","selenio_ug"
		,"zinco_mg","cloro_mg","fluor_mg","molibdenio_ug","cromo_ug","tiamina_mg"
		,"riboflavina_mg","niacina_mg","acido_pantotenico_mg","piridoxina_mg","biotina_ug","acido_folico_ug"
		,"vitamina_b12_ug","vitamina_a_ug","vitamina_a_ui","carotenoides_mg","vitamina_c_mg","vitamina_d_ug"
		,"vitamina_d_ui","vitamina_e_mg","vitamina_e_ui","vitamina_k_ug","isoflavonas_mg","inositol_mg"
		,"colina_mg","carga_solutos_renal_mosm_l");
}
