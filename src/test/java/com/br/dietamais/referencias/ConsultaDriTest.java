package com.br.dietamais.referencias;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Roda {@code contrato/calculos/dri.json}, o mesmo arquivo do mobile (RN-CAL-06), contra o CSV que o Liquibase carrega. */
class ConsultaDriTest {
	final TabelaDri tabela=TabelaDri.doCsv(getClass().getClassLoader().getResourceAsStream(TabelaDri.CSV));

	@Test void todosOsVetoresDeDriBatem() throws Exception {
		var raiz=JsonMapper.builder().build().readTree(getClass().getResourceAsStream("/contrato/calculos/dri.json"));
		var falhas=new ArrayList<String>();int total=0;
		for(JsonNode caso:raiz.path("casos")){
			total++;
			try{rodar(caso);}catch(AssertionError|RuntimeException ex){falhas.add(caso.path("id").asString()+": "+ex.getMessage());}
		}
		assertThat(total).isGreaterThan(50);
		assertThat(falhas).isEmpty();
	}
	void rodar(JsonNode caso){
		var e=caso.path("entrada");
		var obtido=new LinkedHashMap<String,BigDecimal>();
		tabela.metas(e.path("sexo").asString(),e.path("idadeMeses").asInt(),e.path("gestante").asBoolean(),e.path("lactante").asBoolean()).forEach(v->obtido.put(v.nutriente(),v.valor()));
		var esperado=new LinkedHashMap<String,BigDecimal>();
		caso.path("esperado").properties().forEach(p->esperado.put(p.getKey(),new BigDecimal(p.getValue().asString())));
		assertThat(obtido.keySet()).as("nutrientes").containsExactlyInAnyOrderElementsOf(esperado.keySet());
		esperado.forEach((k,v)->assertThat(obtido.get(k)).as(k).isCloseTo(v,org.assertj.core.data.Offset.offset(new BigDecimal("0.01"))));
	}

	@Test void adultoTemOsNutrientesDaPlanilhaComUnidadeCorrigida(){
		var m=tabela.metas("feminino",30*12,false,false);
		assertThat(m).hasSize(28);
		assertThat(tabela.meta("potassio","feminino",360,false,false).orElseThrow().unidade()).isEqualTo("mg/dia");
		assertThat(tabela.meta("sodio","masculino",600,false,false).orElseThrow().valor()).isEqualByComparingTo("1500"); // 50 anos: o buraco da macro
		assertThat(tabela.meta("vitamina_a","feminino",360,false,false).orElseThrow().unidade()).isEqualTo("mcg/dia");
		assertThat(tabela.meta("cobre","feminino",360,false,false).orElseThrow().valor()).isEqualByComparingTo("900"); // correção 7: 1300 só para lactante
		assertThat(tabela.meta("cobre","feminino",360,false,true).orElseThrow().valor()).isEqualByComparingTo("1300");
	}
	@Test void semReferenciaNaoViraZero(){
		assertThat(tabela.meta("fibras","masculino",3,false,false)).isEmpty();
	}
}
