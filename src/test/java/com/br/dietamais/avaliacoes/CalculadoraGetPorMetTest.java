package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.avaliacoes.api.NivelAtividade;
import com.br.dietamais.avaliacoes.dominio.energia.CalculadoraGetPorMet;
import com.br.dietamais.avaliacoes.dominio.energia.CalculadoraGetPorMet.*;
import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.util.*;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Roda {@code contrato/calculos/met.json}, o mesmo arquivo do mobile (RN-CAL-06), contra o CSV de MET que o Liquibase carrega. */
class CalculadoraGetPorMetTest {
	static final Offset<BigDecimal> TOL=Offset.offset(new BigDecimal("0.01"));
	final TabelaAtividadesMet tabela=TabelaAtividadesMet.doCsv(getClass().getClassLoader().getResourceAsStream(TabelaAtividadesMet.CSV));

	@Test void tabelaTemAs113AtividadesComCodigoUnico(){
		assertThat(tabela.listar()).hasSize(113);
		assertThat(tabela.listar().stream().map(AtividadeMet::codigo).distinct()).hasSize(113);
		assertThat(tabela.listar()).allSatisfy(a->assertThat(a.met()).isPositive());
	}

	@Test void todosOsVetoresDeMetBatem() throws Exception {
		var raiz=JsonMapper.builder().build().readTree(getClass().getResourceAsStream("/contrato/calculos/met.json"));
		var falhas=new ArrayList<String>();int total=0;
		for(JsonNode caso:raiz.path("casos")){
			total++;
			try{rodar(caso);}catch(AssertionError|RuntimeException ex){falhas.add(caso.path("id").asString()+": "+ex.getMessage());}
		}
		assertThat(total).isGreaterThan(8);
		assertThat(falhas).isEmpty();
	}
	void rodar(JsonNode caso){
		var e=caso.path("entrada");var esp=caso.path("esperado");
		switch(caso.path("funcao").asString()){
			case "metDaAtividade"->assertThat(tabela.buscar(e.path("codigo").asString()).orElseThrow().met()).isEqualByComparingTo(new BigDecimal(esp.asString()));
			case "getPorMet"->{
				var atividades=new ArrayList<AtividadeDia>();
				for(JsonNode a:e.path("atividades"))atividades.add(new AtividadeDia(tabela.buscar(a.path("codigo").asString()).orElseThrow().met(),dec(a,"minutos")));
				var r=CalculadoraGetPorMet.calcular(new Entrada(e.path("sexo").asString(),dec(e,"pesoKg"),dec(e,"massaMagraKg"),dec(e,"horasSono"),dec(e,"horasTrabalho"),
					NivelAtividade.valueOf(e.path("intensidadeTrabalho").asString()),atividades));
				num(r.gebDia(),esp.path("gebDia"));num(r.gebHora(),esp.path("gebHora"));num(r.gastoSono(),esp.path("gastoSono"));num(r.gastoTrabalho(),esp.path("gastoTrabalho"));
				num(r.horasRotina(),esp.path("horasRotina"));num(r.gastoRotina(),esp.path("gastoRotina"));num(r.get(),esp.path("get"));
				assertThat(r.gastoAtividades()).hasSize(esp.path("gastoAtividades").size());
				for(int i=0;i<r.gastoAtividades().size();i++)num(r.gastoAtividades().get(i),esp.path("gastoAtividades").get(i));
			}
			default->throw new AssertionError("função desconhecida");
		}
	}
	static BigDecimal dec(JsonNode n,String campo){return new BigDecimal(n.path(campo).asString());}
	static void num(BigDecimal obtido,JsonNode esperado){assertThat(obtido).isCloseTo(new BigDecimal(esperado.asString()),TOL);}

	@Test void horasQuePassamDe24Recusam(){
		assertThatThrownBy(()->CalculadoraGetPorMet.calcular(new Entrada("masculino",BigDecimal.valueOf(80),BigDecimal.valueOf(60),BigDecimal.valueOf(10),BigDecimal.valueOf(10),NivelAtividade.LEVE,
			List.of(new AtividadeDia(BigDecimal.valueOf(6),BigDecimal.valueOf(300)))))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("24");
	}
	@Test void maisDeCincoAtividadesRecusa(){
		var a=new AtividadeDia(BigDecimal.valueOf(6),BigDecimal.TEN);
		assertThatThrownBy(()->CalculadoraGetPorMet.calcular(new Entrada("feminino",BigDecimal.valueOf(60),BigDecimal.valueOf(40),BigDecimal.valueOf(8),BigDecimal.valueOf(4),NivelAtividade.LEVE,List.of(a,a,a,a,a,a))))
			.isInstanceOf(IllegalArgumentException.class);
	}
	@Test void trabalhoSedentarioNaoExisteNaAbaMet(){
		assertThatThrownBy(()->CalculadoraGetPorMet.calcular(new Entrada("feminino",BigDecimal.valueOf(60),BigDecimal.valueOf(40),BigDecimal.valueOf(8),BigDecimal.valueOf(4),NivelAtividade.SEDENTARIO,List.of())))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
