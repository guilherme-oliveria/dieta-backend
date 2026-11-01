package com.br.dietamais.referencias;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Catálogo e regras de exames (BE-56a) contra as fórmulas da aba av. bioquimica. Os mesmos CSVs do Liquibase. */
class TabelaExamesTest {
	static final TabelaExames T=TabelaExames.doCsv(TabelaExamesTest.class.getClassLoader().getResourceAsStream(TabelaExames.CSV_CATALOGO),TabelaExamesTest.class.getClassLoader().getResourceAsStream(TabelaExames.CSV_REGRAS));
	String c(String faixa,String codigo,String valor,String sexo,Double idade,String condicao){
		var r=T.classificar(faixa,codigo,new BigDecimal(valor),null,sexo,idade,condicao).orElseThrow();return r.rotulo()!=null?r.rotulo():"("+r.motivo()+")";
	}

	@Test void catalogosPorFaixaEtaria(){
		assertThat(T.faixasEtarias()).containsExactlyInAnyOrder("CRIANCA","ADOLESCENTE","ADULTO");
		assertThat(T.catalogo("ADULTO")).hasSize(132);assertThat(T.catalogo("ADOLESCENTE")).hasSize(132);assertThat(T.catalogo("CRIANCA")).hasSize(131);
		var ferritina=T.exame("ADULTO","ferritina").orElseThrow();
		assertThat(ferritina.unidade()).isEqualTo("ng/ml");assertThat(ferritina.condicoes()).contains("menarca","pós-menopausa","homem");
		assertThat(T.exame("ADULTO","ferritina").orElseThrow().secao()).isEqualTo("Exames de rotina básicos em consultório");
	}
	@Test void ladosDaFaixaSeguemAsFormulasDaPlanilha(){
		// Ácido úrico feminino: < 2,4 baixo; = 2,4 limite inferior; entre normal; = 6 limite superior; > 6 alto
		assertThat(c("ADULTO","acido-urico","2.3","feminino",null,null)).isEqualTo("baixo");
		assertThat(c("ADULTO","acido-urico","2.4","feminino",null,null)).isEqualTo("limite inferior");
		assertThat(c("ADULTO","acido-urico","4","feminino",null,null)).isEqualTo("normal");
		assertThat(c("ADULTO","acido-urico","6","feminino",null,null)).isEqualTo("limite superior");
		assertThat(c("ADULTO","acido-urico","6.1","feminino",null,null)).isEqualTo("alto");
		// Glicemia de jejum: 100 a 125 intolerância; acima de 125 diabetes
		assertThat(c("ADULTO","glicemia-de-jejum","99","masculino",null,null)).isEqualTo("normal");
		assertThat(c("ADULTO","glicemia-de-jejum","100","masculino",null,null)).isEqualTo("int. à glicose");
		assertThat(c("ADULTO","glicemia-de-jejum","125","masculino",null,null)).isEqualTo("int. à glicose");
		assertThat(c("ADULTO","glicemia-de-jejum","126","masculino",null,null)).isEqualTo("diabetes");
		// Triglicérides: < 150 ótimo; 150 a 200 limítrofe; 200 a 500 alto; >= 500 muito alto
		assertThat(c("ADULTO","triglicerides","150","masculino",null,null)).isEqualTo("limítrofe");
		assertThat(c("ADULTO","triglicerides","500","masculino",null,null)).isEqualTo("muito alto");
		// HbA1c: o limite depende da idade na data do exame (60 anos ou mais)
		assertThat(c("ADULTO","hemoglobina-glicada-hba1c","7","feminino",59.0,null)).isEqualTo("alto");
		assertThat(c("ADULTO","hemoglobina-glicada-hba1c","7","feminino",60.0,null)).isEqualTo("normal");
	}
	@Test void ferritinaDependeDaFaseEscolhidaESemElaNaoHaPalpite(){
		assertThat(c("ADULTO","ferritina","10","feminino",null,"menarca")).isEqualTo("limite inferior");
		assertThat(c("ADULTO","ferritina","9","feminino",null,"menarca")).isEqualTo("baixo");
		assertThat(c("ADULTO","ferritina","10","feminino",null,null)).isEqualTo("(FALTA_CONDICAO)");
		assertThat(c("ADULTO","acido-urico","4",null,null,null)).isEqualTo("(FALTA_SEXO)");
	}
	@Test void qualitativoUsaTextoEForaDaListaCaiNoAlterado(){
		assertThat(T.classificar("ADULTO","glicose",null,"ausente",null,null,null).orElseThrow().rotulo()).isEqualTo("normal");
		assertThat(T.classificar("ADULTO","glicose",null,"presente",null,null,null).orElseThrow().rotulo()).isEqualTo("alterado");
	}
	@Test void exameInexistenteNaFaixaDevolveVazio(){
		assertThat(T.classificar("ADULTO","nao-existe",BigDecimal.ONE,null,null,null,null)).isEmpty();
		assertThat(T.exame("IDOSO","ferritina")).isEmpty();
	}
	@Test void faixasValemParaOPacienteESemDadoNaoHaChute(){
		var diabetes=T.faixas("ADULTO","glicemia-de-jejum","masculino",30.0,null).stream().filter(f->"diabetes".equals(f.rotulo())).findFirst().orElseThrow();
		assertThat(diabetes.min()).isEqualByComparingTo("125");assertThat(diabetes.minIncl()).isFalse();assertThat(diabetes.max()).isNull();
		assertThat(T.faixas("ADULTO","glicemia-de-jejum","masculino",30.0,null)).extracting(f->f.rotulo()).contains("normal","int. à glicose","diabetes");
		assertThat(T.faixas("ADULTO","ferritina","feminino",30.0,null)).isEmpty();
		assertThat(T.faixas("ADULTO","ferritina","feminino",30.0,"menarca")).isNotEmpty();
		assertThat(T.faixas("ADULTO","nao-existe","feminino",30.0,null)).isEmpty();
	}
}
