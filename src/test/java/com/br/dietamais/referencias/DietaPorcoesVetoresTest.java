package com.br.dietamais.referencias;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Confere {@code contrato/calculos/dieta-porcoes.json} contra os CSVs de porções: total = Σ porções × perfil da porção (BE-C8). */
class DietaPorcoesVetoresTest {
	List<String[]> ler(String nome) throws Exception {
		try(var in=getClass().getResourceAsStream("/db/changelog/dados/"+nome)){
			var l=new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\n");var r=new ArrayList<String[]>();
			for(int i=1;i<l.length;i++)r.add(l[i].split(";",-1));
			return r;
		}
	}
	@Test void totaisDoVetorBatemComSomaDosPerfis() throws Exception {
		var porcoes=ler("grupo-porcao.csv");var nutrientes=ler("grupo-porcao-nutriente.csv");
		var raiz=JsonMapper.builder().build().readTree(getClass().getResourceAsStream("/contrato/calculos/dieta-porcoes.json"));
		var dia=new HashMap<String,BigDecimal>();
		for(JsonNode caso:raiz.path("casos")){
			var soma=new HashMap<String,BigDecimal>();
			for(JsonNode item:caso.path("itens")){
				var grupo=String.format("019a0000-0000-7000-8000-%012d",item.path("grupo").asInt());
				var id=porcoes.stream().filter(c->c[1].equals(grupo)&&c[2].equals(String.valueOf(item.path("opcao").asInt()))).findFirst().orElseThrow()[0];
				var q=new BigDecimal(item.path("porcoes").asString());
				for(var n:nutrientes)if(n[0].equals(id))soma.merge(n[1],new BigDecimal(n[2]).multiply(q),BigDecimal::add);
			}
			for(var e:caso.path("totais").properties()){
				assertThat(soma.get(e.getKey()).setScale(2,RoundingMode.HALF_UP).subtract(new BigDecimal(e.getValue().asString())).abs()).as(caso.path("refeicao").asString()+" "+e.getKey()).isLessThanOrEqualTo(new BigDecimal("0.01"));
			}
			soma.forEach((k,v)->dia.merge(k,v,BigDecimal::add));
		}
		for(var e:raiz.path("dia").properties())assertThat(dia.get(e.getKey()).setScale(2,RoundingMode.HALF_UP).subtract(new BigDecimal(e.getValue().asString())).abs()).as("dia "+e.getKey()).isLessThanOrEqualTo(new BigDecimal("0.02"));
	}
}
