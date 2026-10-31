package com.br.dietamais.relatorios;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.aplicacao.CalculoAvaliacao;
import com.br.dietamais.pacientes.api.*;
import com.br.dietamais.referencias.api.*;
import com.br.dietamais.relatorios.aplicacao.GeradorPdf;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** BE-46 sem banco: o PDF da avaliação traz as seções da planilha, os dados calculados e a energia, e some com o que falta (RN-CAL-04). */
class PdfAvaliacaoTest {
	final CalculoAvaliacao calculo=new CalculoAvaliacao(TabelaFaixas.doCsv(getClass().getClassLoader().getResourceAsStream(TabelaFaixas.CSV_ADULTO)),
		TabelaAtividadesMet.doCsv(getClass().getClassLoader().getResourceAsStream(TabelaAtividadesMet.CSV)));
	static final UUID ID=UUID.randomUUID();
	final Instant quando=ZonedDateTime.of(2026,10,31,12,0,0,0,ZoneId.of("America/Sao_Paulo")).toInstant();
	final GeradorPdf.Cabecalho cabecalho=new GeradorPdf.Cabecalho("Nutri fictícia","CRN-1 12345","Paciente fictício",null);

	AvaliacaoResposta avaliacao(boolean completa){
		var compl=new MedidasComplementaresAvaliacao(null,null,new BigDecimal("18"),completa?new BigDecimal("6.5"):null,completa?new BigDecimal("9.5"):null,new BigDecimal("18"),NivelAtividade.MODERADO,null,null,null);
		var energia=new EnergiaAvaliacao(MetodoGet.FAO_2004,null,null,null,null,null,null,null,null);
		var r=new SalvarAvaliacaoRequisicao(null,quando,new BigDecimal("102.5"),new BigDecimal("1.93"),new PerimetrosAvaliacao(null,null,new BigDecimal("100"),null,null,new BigDecimal("36"),null,null,null),
			new DobrasAvaliacao(new BigDecimal("20"),new BigDecimal("30"),new BigDecimal("25"),new BigDecimal("28"),new BigDecimal("32"),new BigDecimal("40"),new BigDecimal("24")),
			null,"Parecer fictício",true,null,null,compl,null,energia);
		var ficha=new FichaParaLista(ID,ID,StatusVinculo.ATIVO,LocalDate.of(1997,4,1),Sexo.MASCULINO);
		var c=calculo.calcular(r,ficha,null,new BigDecimal("27.52"));
		return new AvaliacaoResposta(ID,ID,ID,quando,r.pesoKg(),r.estaturaM(),new BigDecimal("27.52"),ClassificacaoImc.SOBREPESO,r.perimetros(),r.dobras(),ComposicaoAvaliacao.VAZIO,r.parecer(),true,1L,quando,quando,
			c.faixaEtaria(),c.protocolo(),r.complementares(),c.resultados(),null,r.energia(),c.energia());
	}
	String texto(byte[] pdf) throws Exception {
		try(var doc=org.apache.pdfbox.pdmodel.PDDocument.load(pdf)){return new org.apache.pdfbox.text.PDFTextStripper().getText(doc);}
	}

	@Test void pdfCompletoTemMedidasPerimetrosDobrasComposicaoEnergiaEReferencias() throws Exception {
		var pdf=new GeradorPdf().avaliacao(cabecalho,avaliacao(true));
		assertThat(new String(pdf,0,5)).isEqualTo("%PDF-");
		var t=texto(pdf);
		assertThat(t).contains("Avaliação física","Paciente fictício","Idade: 29 anos","102,5 kg","1,93 m","27,5 kg/m² (sobrepeso)","Faixa teórica","Peso e faixa teórica");
		assertThat(t).contains("Perímetros (cm)","Cintura","Risco pela cintura","Dobras cutâneas (mm)","Somatório das dobras: 199,0 mm");
		assertThat(t).contains("Composição corporal","Gordura","26,5 %","(alto)","Peso gordo","Peso ósseo","Peso residual","Peso para o % de gordura desejado");
		assertThat(t).contains("Necessidades de energia","FAO/OMS 2004","2.236 kcal","3.935 kcal","% do VET","Referências","Magreza grau III","Parecer fictício","Resumo");
		assertThat(t).contains("Nutri fictícia - CRN-1 12345");
	}
	@Test void semDadosSomeASecaoENuncaViraZero() throws Exception {
		var base=avaliacao(false);
		var semResultados=new AvaliacaoResposta(ID,ID,ID,quando,base.pesoKg(),base.estaturaM(),base.imc(),base.classificacaoImc(),PerimetrosAvaliacao.VAZIO,DobrasAvaliacao.VAZIO,ComposicaoAvaliacao.VAZIO,null,true,1L,quando,quando,null,null,null,null,null);
		var t=texto(new GeradorPdf().avaliacao(cabecalho,semResultados));
		assertThat(t).contains("Medidas","Referências").doesNotContain("Perímetros","Dobras cutâneas","Composição corporal","Necessidades de energia","Parecer");
	}
}
