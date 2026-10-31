package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.dominio.energia.CalculadoraEnergia;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Roda {@code contrato/calculos/energia.json}, o mesmo arquivo do mobile (RN-CAL-06). */
class CalculadoraEnergiaTest {
	static final BigDecimal TOL=new BigDecimal("0.01");

	@Test void todosOsVetoresDeEnergiaBatem() throws Exception {
		var raiz=JsonMapper.builder().build().readTree(getClass().getResourceAsStream("/contrato/calculos/energia.json"));
		var falhas=new ArrayList<String>();int total=0;
		for(JsonNode caso:raiz.path("casos")){
			total++;
			try{rodar(caso);}catch(AssertionError|RuntimeException ex){falhas.add(caso.path("id").asString()+": "+ex.getMessage());}
		}
		assertThat(total).isGreaterThan(60);
		assertThat(falhas).isEmpty();
	}

	void rodar(JsonNode caso){
		var e=caso.path("entrada");var esp=caso.path("esperado");
		switch(caso.path("funcao").asString()){
			case "gebFao1985"->num(CalculadoraEnergia.gebFao1985(e.path("sexo").asString(),e.path("idadeAnos").asInt(),dec(e,"pesoKg")),esp);
			case "gebFao2004"->num(CalculadoraEnergia.gebFao2004(e.path("sexo").asString(),e.path("idadeAnos").asInt(),dec(e,"pesoKg")),esp);
			case "gebIom2002"->num(CalculadoraEnergia.gebIom2002(e.path("sexo").asString(),e.path("idadeAnos").asInt(),dec(e,"pesoKg"),dec(e,"estaturaM"),e.path("sobrepesoOuObeso").asBoolean()),esp);
			case "gebCunningham"->num(CalculadoraEnergia.gebCunningham(dec(e,"massaMagraKg")),esp);
			case "getFao1985"->num(CalculadoraEnergia.getFao1985(dec(e,"gebKcal"),e.path("sexo").asString(),NivelAtividade.valueOf(e.path("nivel").asString())),esp);
			case "getFao2004"->num(CalculadoraEnergia.getFao2004(dec(e,"gebKcal"),NivelAtividade.valueOf(e.path("nivel").asString()),e.path("lactante").asBoolean()),esp);
			case "getIom2005"->num(CalculadoraEnergia.getIom2005(dec(e,"gebIomKcal"),NivelAtividade.valueOf(e.path("nivel").asString())),esp);
			case "getRdi2005"->num(CalculadoraEnergia.getRdi2005(e.path("sexo").asString(),e.path("idadeAnos").asInt(),dec(e,"pesoKg"),dec(e,"estaturaM"),
				NivelAtividade.valueOf(e.path("nivel").asString()),e.path("periodoLactacao").isNull()?null:PeriodoLactacao.valueOf(e.path("periodoLactacao").asString())),esp);
			case "vet"->num(CalculadoraEnergia.vet(dec(e,"getKcal"),ObjetivoPeso.valueOf(e.path("objetivo").asString()),e.path("kgPorMes").isNull()?null:dec(e,"kgPorMes")),esp);
			case "kcalPorKg"->num(CalculadoraEnergia.kcalPorKg(dec(e,"vetKcal"),dec(e,"pesoKg")),esp);
			case "carboidrato"->meta(CalculadoraEnergia.carboidrato(ProtocoloCarboidrato.valueOf(e.path("protocolo").asString()),dec(e,"vetKcal"),dec(e,"pesoKg"),
				e.path("gestante").asBoolean(),e.path("lactante").asBoolean()),esp);
			case "proteina"->meta(CalculadoraEnergia.proteina(ProtocoloProteina.valueOf(e.path("protocolo").asString()),dec(e,"vetKcal"),dec(e,"pesoKg"),e.path("sexo").asString(),
				e.path("idadeAnos").asInt(),e.path("gestante").asBoolean(),e.path("lactante").asBoolean()),esp);
			case "lipidio"->meta(CalculadoraEnergia.lipidio(ProtocoloLipidio.valueOf(e.path("protocolo").asString()),dec(e,"vetKcal")),esp);
			case "lipidiosDislipidemico"->{
				var l=CalculadoraEnergia.lipidiosDislipidemico(dec(e,"vetKcal"));
				meta(l.total(),esp.path("total"));meta(l.saturado(),esp.path("saturado"));meta(l.monoinsaturado(),esp.path("monoinsaturado"));
				meta(l.poliinsaturado(),esp.path("poliinsaturado"));meta(l.trans(),esp.path("trans"));
			}
			case "colesterolMaximoMg"->num(CalculadoraEnergia.colesterolMaximoMg(e.path("colesterolTotal").isNull()?null:dec(e,"colesterolTotal")),esp);
			default->throw new AssertionError("função desconhecida");
		}
	}
	static BigDecimal dec(JsonNode n,String campo){return new BigDecimal(n.path(campo).asString());}
	static void num(BigDecimal obtido,JsonNode esperado){
		assertThat(obtido).as("valor").isCloseTo(new BigDecimal(esperado.asString()),org.assertj.core.data.Offset.offset(TOL));
	}
	static void meta(MetaMacro m,JsonNode esp){
		campo(m.percentualMinimo(),esp.path("percentualMinimo"));campo(m.percentualMaximo(),esp.path("percentualMaximo"));
		campo(m.gramasMinimo(),esp.path("gramasMinimo"));campo(m.gramasMaximo(),esp.path("gramasMaximo"));
	}
	static void campo(BigDecimal obtido,JsonNode esp){
		if(esp.isNull())assertThat(obtido).isNull();else num(obtido,esp);
	}

	@Test void idadeAbaixoDe18Recusa(){
		assertThatThrownBy(()->CalculadoraEnergia.gebFao1985("masculino",17,BigDecimal.TEN)).isInstanceOf(IllegalArgumentException.class);
	}
	@Test void sexoDesconhecidoRecusa(){
		assertThatThrownBy(()->CalculadoraEnergia.gebFao2004("outro",30,BigDecimal.TEN)).isInstanceOf(IllegalArgumentException.class);
	}
}
