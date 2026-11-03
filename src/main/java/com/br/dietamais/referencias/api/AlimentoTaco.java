package com.br.dietamais.referencias.api;

import java.math.BigDecimal;
import java.util.*;

/**
 * Alimento da TACO por 100 g de parte comestível (BE-47a). {@code nutrientes} só traz o que foi analisado (RN-CAL-04: sem dado não vira zero);
 * {@code tracos} lista os nutrientes que a fonte marca como traço (valor 0 na tabela). Chaves: umidade_g, energia_kcal, energia_kj, proteina_g,
 * lipideos_g, colesterol_mg, carboidrato_g, fibra_g, cinzas_g, calcio_mg, magnesio_mg, manganes_mg, fosforo_mg, ferro_mg, sodio_mg, potassio_mg,
 * cobre_mg, zinco_mg, retinol_ug, re_ug, rae_ug, tiamina_mg, riboflavina_mg, piridoxina_mg, niacina_mg, vitamina_c_mg, saturados_g,
 * monoinsaturados_g, poliinsaturados_g.
 */
public record AlimentoTaco(int numero,String nome,String categoria,Map<String,BigDecimal> nutrientes,List<String> tracos){
	public static final String FONTE="TACO — Tabela Brasileira de Composição de Alimentos, 4ª edição revista e ampliada, NEPA-UNICAMP, 2011";
}
