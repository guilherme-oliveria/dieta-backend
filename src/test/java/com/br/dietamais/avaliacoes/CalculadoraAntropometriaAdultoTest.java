package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.dominio.*;
import com.br.dietamais.referencias.api.TabelaFaixas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Roda {@code contrato/calculos/adulto.json}, o mesmo arquivo do mobile (RN-CAL-06), contra as tabelas do CSV do Liquibase. */
class CalculadoraAntropometriaAdultoTest {
	final CalculadoraAntropometriaAdulto calc=new CalculadoraAntropometriaAdulto(TabelaFaixas.doCsv(getClass().getClassLoader().getResourceAsStream(TabelaFaixas.CSV_ADULTO)));
	JsonNode raiz;
	Map<String,Double> tol=new HashMap<>();

	@Test void todosOsVetoresDoAdultoBatem() throws Exception {
		raiz=JsonMapper.builder().build().readTree(getClass().getResourceAsStream("/contrato/calculos/adulto.json"));
		raiz.path("tolerancia").properties().forEach(e->tol.put(e.getKey(),e.getValue().asDouble()));
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
			case "classificarImcAdulto"->assertThat(calc.classificarImc(30,dec(e,"imc")).name()).isEqualTo(esp.asString());
			case "classificarImcIdoso"->assertThat(calc.classificarImc(e.path("idadeAnos").asInt(),dec(e,"imc")).name()).isEqualTo(esp.asString());
			case "classificarCintura"->assertThat(calc.classificarCintura(e.path("sexo").asString(),dec(e,"cinturaCm")).name()).isEqualTo(esp.asString());
			case "classificarCompleicao"->assertThat(calc.classificarCompleicao(e.path("sexo").asString(),dec(e,"estaturaM"),dec(e,"punhoCm")).name()).isEqualTo(esp.asString());
			case "classificarPercentualGordura"->assertThat(calc.classificarPercentualGordura(e.path("sexo").asString(),e.path("idadeAnos").asInt(),dec(e,"percentualGordura")).map(Enum::name).orElse(null))
				.isEqualTo(esp.isNull()?null:esp.asString());
			case "calcularIdade"->assertThat(CalculadoraAntropometriaAdulto.idadeEmAnos(ChronoUnit.DAYS.between(LocalDate.parse(e.path("nascimento").asString()),LocalDate.parse(e.path("dataAvaliacao").asString())))).isEqualTo(esp.asInt());
			case "avaliacaoAdulto"->avaliacao(e,esp);
			default->fail("função desconhecida "+caso.path("funcao"));
		}
	}
	void avaliacao(JsonNode e,JsonNode esp){
		var d=e.path("dobras");
		var entrada=new EntradaAvaliacaoAdulto(e.path("sexo").asString(),e.path("idadeAnos").asInt(),dec(e,"pesoKg"),dec(e,"estaturaM"),
			ProtocoloDobras.valueOf(e.path("protocoloDobras").asString()),ProtocoloMassa.valueOf(e.path("protocoloMassa").asString()),
			dec(d,"triceps"),dec(d,"peitoral"),dec(d,"axilarMedia"),dec(d,"subescapular"),dec(d,"abdominal"),dec(d,"suprailiaca"),dec(d,"coxa"),
			dec(e,"cinturaCm"),dec(e,"punhoCm"),dec(e,"diametroPunhoCm"),dec(e,"diametroJoelhoCm"),dec(e,"percentualDesejado"));
		var r=calc.avaliar(entrada);var x=r.resultados();
		num(esp,"imc",r.imc(),"imc");txt(esp,"classificacaoImc",x.classificacaoImc());txt(esp,"riscoComorbidades",x.riscoComorbidades());
		num(esp,"pesoTeoricoMinKg",x.pesoTeoricoMinKg(),"kg");num(esp,"pesoTeoricoMaxKg",x.pesoTeoricoMaxKg(),"kg");
		txt(esp,"compleicao",x.compleicao());txt(esp,"classificacaoCintura",x.classificacaoCintura());
		num(esp,"somatorioDobrasMm",x.somatorioDobrasMm(),"mm");num(esp,"densidadeCorporal",x.densidadeCorporal(),"densidade");
		num(esp,"percentualGordura",x.percentualGordura(),"percentual");txt(esp,"classificacaoPercentualGordura",x.classificacaoPercentualGordura());
		num(esp,"percentualMagro",x.percentualMagro(),"percentual");num(esp,"pesoGordoKg",x.pesoGordoKg(),"kg");num(esp,"pesoOsseoKg",x.pesoOsseoKg(),"kg");
		num(esp,"pesoResidualKg",x.pesoResidualKg(),"kg");num(esp,"pesoMagroKg",x.pesoMagroKg(),"kg");num(esp,"pesoParaPercentualDesejadoKg",x.pesoParaPercentualDesejadoKg(),"kg");
		assertThat(x.textoResumo()).as("textoResumo").isEqualTo(esp.path("textoResumo").isNull()?null:esp.path("textoResumo").asString());
		var motivos=new HashMap<String,String>();esp.path("motivosNulos").properties().forEach(m->motivos.put(m.getKey(),m.getValue().asString()));
		assertThat(x.motivosNulos()).as("motivosNulos").isEqualTo(motivos);
	}
	void num(JsonNode esp,String campo,BigDecimal obtido,String t){
		var v=esp.path(campo);
		if(v.isNull()||v.isMissingNode()){assertThat(obtido).as(campo).isNull();return;}
		assertThat(obtido).as(campo).isNotNull();
		assertThat(Math.abs(obtido.doubleValue()-v.asDouble())).as(campo+": esperado "+v.asDouble()+", obtido "+obtido).isLessThanOrEqualTo(tol.get(t)+1e-9);
	}
	void txt(JsonNode esp,String campo,Enum<?> obtido){
		var v=esp.path(campo);assertThat(obtido==null?null:obtido.name()).as(campo).isEqualTo(v.isNull()||v.isMissingNode()?null:v.asString());
	}
	static BigDecimal dec(JsonNode n,String campo){var v=n.path(campo);return v.isMissingNode()||v.isNull()?null:new BigDecimal(v.asString());}
}
