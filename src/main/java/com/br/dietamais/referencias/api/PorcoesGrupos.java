package com.br.dietamais.referencias.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.*;

/**
 * Porções por grupo da dieta (BE-47b), como na aba "Escolha as porções" da planilha "Dieta sem restrições".
 * Os valores são a média dos alimentos do grupo na porção e são provisórios (visao-geral, suposição S-porções).
 */
@Schema(description="Grupos da dieta com as porções que a nutri pode escolher e o perfil de cada porção")
public record PorcoesGrupos(
	@Schema(description="Fonte e aviso dos valores, para o app e o PDF mostrarem") String fonte,
	@Schema(description="Unidade de cada código de nutriente (por porção)") Map<String,String> unidades,
	List<Grupo> grupos){
	public static final String FONTE="Médias dos alimentos de cada grupo na planilha \"Dieta sem restrições\" da Nut. Luciana (valores provisórios); alimentos individuais: TACO 4ª ed. (NEPA-UNICAMP).";
	/** Código do nutriente e unidade, na ordem da planilha (colunas F a AM da aba Alimentos). */
	public static final Map<String,String> UNIDADES;
	static{
		var m=new LinkedHashMap<String,String>();
		String[] c={"energia","gli","pro","lip","sat","mon","pol","trans","col","fib","na","k","ca","cu","i","mg","mn","p","se","zn","fe","fol","nia","pan","b2","b1","ret","vita","b6","b12","c","d","e","agua"};
		String[] u={"kcal","g","g","g","g","g","g","g","mg","g","mg","mg","mg","mcg","mcg","mg","mg","mg","mcg","mg","mg","mcg","mg","mg","mg","mg","mcg","mcg","mg","mcg","mg","mcg","mg","g"};
		for(int i=0;i<c.length;i++)m.put(c[i],u[i]);
		UNIDADES=Collections.unmodifiableMap(m);
	}
	public record Grupo(UUID grupoId,String nome,List<Opcao> opcoes){}
	@Schema(description="Uma porção do grupo. `nutrientes` só traz o que algum alimento do grupo informa (nunca zero por falta de dado).")
	public record Opcao(int opcao,BigDecimal kcal,BigDecimal quantidade,String unidade,String medidaCaseira,boolean guiaAlimentar,Map<String,BigDecimal> nutrientes){}
}
