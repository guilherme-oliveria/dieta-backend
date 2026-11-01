package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.avaliacoes.dominio.infantil.CalculadoraInfantil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Roda {@code contrato/calculos/infantil.json}, o mesmo arquivo do mobile (RN-CAL-06). */
class CalculadoraInfantilTest {
	static final BigDecimal TOL=new BigDecimal("0.01");

	@Test void todosOsVetoresInfantisBatem() throws Exception {
		var raiz=JsonMapper.builder().build().readTree(getClass().getResourceAsStream("/contrato/calculos/infantil.json"));
		var falhas=new ArrayList<String>();int total=0;
		for(JsonNode caso:raiz.path("casos")){
			total++;
			try{rodar(caso);}catch(AssertionError|RuntimeException ex){falhas.add(caso.path("id").asString()+": "+ex.getMessage());}
		}
		assertThat(total).isGreaterThan(100);
		assertThat(falhas).isEmpty();
	}

	void rodar(JsonNode caso){
		var e=caso.path("entrada");var esp=caso.path("esperado");
		switch(caso.path("funcao").asString()){
			case "pesoNascer"->igual(CalculadoraInfantil.classificarPesoNascer(e.path("gramas").asInt()),esp);
			case "adequacao"->{
				var p=CalculadoraInfantil.adequacao(dec(e,"medida"),dec(e,"p50"));
				num(p,esp.path("percentual"));
				assertThat(CalculadoraInfantil.classificarAdequacao(p)).hasToString(esp.path("classe").asString());
			}
			case "braco"->{
				num(CalculadoraInfantil.ambc(dec(e,"cb"),dec(e,"dct"),e.path("masculino").asBoolean()),esp.path("ambc"));
				num(CalculadoraInfantil.agb(dec(e,"cb"),dec(e,"dct"),e.path("masculino").asBoolean()),esp.path("agb"));
			}
			case "percentilBraco"->igual(CalculadoraInfantil.classificarPercentilBraco(dec(e,"valor"),dec(e,"p5"),dec(e,"p10"),dec(e,"p90")),esp);
			case "estaturaIdade"->igual(CalculadoraInfantil.classificarEstaturaIdade(dec(e,"valor"),dec(e,"a"),dec(e,"b"),dec(e,"c")),esp);
			case "imcCrianca"->igual(CalculadoraInfantil.classificarImcCrianca(dec(e,"valor"),dec(e,"a"),dec(e,"b"),dec(e,"c"),dec(e,"d"),dec(e,"e")),esp);
			case "imcAdolescente"->igual(CalculadoraInfantil.classificarImcAdolescente(dec(e,"valor"),dec(e,"a"),dec(e,"b"),dec(e,"c")),esp);
			case "slaughter"->num(CalculadoraInfantil.slaughter(e.path("masculino").asBoolean(),dec(e,"triciptal"),dec(e,"subescapular"),inteiro(e,"idade"),inteiro(e,"etnia")),esp);
			case "classeGordura"->igual(CalculadoraInfantil.classificarGordura(e.path("masculino").asBoolean(),dec(e,"percentual")),esp);
			case "gebIom"->num(CalculadoraInfantil.gebIom(e.path("masculino").asBoolean(),dec(e,"idade"),dec(e,"peso"),dec(e,"estatura")),esp);
			case "getIom"->num(CalculadoraInfantil.getIom(e.path("masculino").asBoolean(),e.path("idade").asInt(),dec(e,"af"),dec(e,"peso"),dec(e,"estatura")),esp);
			case "getRdi"->num(CalculadoraInfantil.getRdi(e.path("masculino").asBoolean(),e.path("anos").asInt(),e.path("meses").asInt(),dec(e,"af"),dec(e,"peso"),dec(e,"estatura")),esp);
			case "pesoParaImc"->num(CalculadoraInfantil.pesoParaImc(dec(e,"imc"),dec(e,"estatura")),esp);
			case "idadeTexto"->assertThat(CalculadoraInfantil.idadeTexto(LocalDate.parse(e.path("nascimento").asString()),LocalDate.parse(e.path("data").asString()))).isEqualTo(esp.asString());
			default->throw new AssertionError("função desconhecida");
		}
	}
	static Integer inteiro(JsonNode n,String c){return n.path(c).isNull()?null:n.path(c).asInt();}
	static BigDecimal dec(JsonNode n,String c){return n.path(c).isNull()?null:new BigDecimal(n.path(c).asString());}
	static void igual(Object obtido,JsonNode esperado){
		if(esperado.isNull())assertThat(obtido).isNull();else assertThat(obtido).hasToString(esperado.asString());
	}
	static void num(BigDecimal obtido,JsonNode esperado){
		if(esperado.isNull()){assertThat(obtido).isNull();return;}
		assertThat(obtido).isNotNull();
		assertThat(obtido.subtract(new BigDecimal(esperado.asString())).abs()).isLessThanOrEqualTo(TOL);
	}
}
