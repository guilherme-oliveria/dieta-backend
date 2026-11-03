package com.br.dietamais.questionarios;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.questionarios.dominio.anamnese.CalculadoraAnamnese;
import com.br.dietamais.questionarios.dominio.anamnese.CalculadoraAnamnese.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Roda {@code contrato/calculos/anamnese.json}, o mesmo arquivo do mobile (RN-CAL-06). */
class CalculadoraAnamneseTest {
	static final BigDecimal TOL=new BigDecimal("0.01");

	@Test void todosOsVetoresDaAnamneseBatem() throws Exception {
		var raiz=JsonMapper.builder().build().readTree(getClass().getResourceAsStream("/contrato/calculos/anamnese.json"));
		var falhas=new ArrayList<String>();int total=0;
		for(JsonNode caso:raiz.path("casos")){
			total++;
			try{rodar(caso);}catch(AssertionError|RuntimeException ex){falhas.add(caso.path("id").asString()+": "+ex.getMessage());}
		}
		assertThat(total).isGreaterThan(50);
		assertThat(falhas).isEmpty();
	}

	void rodar(JsonNode caso){
		var e=caso.path("entrada");var esp=caso.path("esperado");
		switch(caso.path("funcao").asString()){
			case "classificarPressao"->{
				var r=CalculadoraAnamnese.classificarPressao(inteiro(e,"sistolica"),inteiro(e,"diastolica"));
				if(esp.isNull())assertThat(r).isNull();else assertThat(r).hasToString(esp.asString());
			}
			case "alcool"->{
				var a=CalculadoraAnamnese.alcool(Bebida.valueOf(e.path("bebida").asString()),UnidadeBebida.valueOf(e.path("unidade").asString()),dec(e,"quantidade"),dec(e,"vezesPorSemana"));
				num(a.mlPorSemana(),esp.path("mlPorSemana"));num(a.gramasEtanolPorSemana(),esp.path("gramasEtanolPorSemana"));num(a.kcalPorSemana(),esp.path("kcalPorSemana"));
			}
			case "salGramasPorPessoaDia"->assertThat(CalculadoraAnamnese.salGramasPorPessoaDia(dec(e,"gramasNoMes"),e.path("pessoas").asInt())).isEqualTo(Integer.parseInt(esp.asString()));
			case "oleo"->{
				assertThat(CalculadoraAnamnese.oleoMlPorPessoaDia(dec(e,"mlNoMes"),e.path("pessoas").asInt())).isEqualTo(Integer.parseInt(esp.path("mlPorPessoaDia").asString()));
				assertThat(CalculadoraAnamnese.oleoKcalPorPessoaDia(dec(e,"mlNoMes"),e.path("pessoas").asInt())).isEqualTo(Integer.parseInt(esp.path("kcalPorPessoaDia").asString()));
			}
			case "duracaoMinutos"->assertThat(CalculadoraAnamnese.duracaoMinutos(LocalTime.parse(e.path("inicio").asString()),LocalTime.parse(e.path("fim").asString()))).isEqualTo(Integer.parseInt(esp.asString()));
			default->throw new AssertionError("função desconhecida");
		}
	}
	static Integer inteiro(JsonNode n,String c){return n.path(c).isNull()?null:n.path(c).asInt();}
	static BigDecimal dec(JsonNode n,String c){return new BigDecimal(n.path(c).asString());}
	static void num(BigDecimal obtido,JsonNode esperado){
		assertThat(obtido).isCloseTo(new BigDecimal(esperado.asString()),org.assertj.core.data.Offset.offset(TOL));
	}

	@Test void somaDeBebidas(){
		var a=CalculadoraAnamnese.alcool(Bebida.CERVEJA,UnidadeBebida.LATINHA,new BigDecimal("2"),new BigDecimal("3"));
		var b=CalculadoraAnamnese.alcool(Bebida.VINHO,UnidadeBebida.TACA_MEDIA,BigDecimal.ONE,new BigDecimal("7"));
		var s=CalculadoraAnamnese.somar(List.of(a,b));
		assertThat(s.mlPorSemana()).isEqualByComparingTo(a.mlPorSemana().add(b.mlPorSemana()));
	}
	@Test void pessoasZeroRecusa(){
		assertThatThrownBy(()->CalculadoraAnamnese.salGramasPorPessoaDia(BigDecimal.TEN,0)).isInstanceOf(IllegalArgumentException.class);
	}
	@Test void pressaoNegativaRecusa(){
		assertThatThrownBy(()->CalculadoraAnamnese.classificarPressao(0,80)).isInstanceOf(IllegalArgumentException.class);
	}
}
