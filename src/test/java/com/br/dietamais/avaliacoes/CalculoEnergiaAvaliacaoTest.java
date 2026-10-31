package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.aplicacao.CalculoAvaliacao;
import com.br.dietamais.avaliacoes.dominio.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.*;
import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** BE-43b sem banco: a escolha da nutri vira GEB, GET, VET e metas gravados pelo servidor (RN-CAL-01, RN-CAL-04). */
class CalculoEnergiaAvaliacaoTest {
	final CalculoAvaliacao calculo=new CalculoAvaliacao(TabelaFaixas.doCsv(getClass().getClassLoader().getResourceAsStream(TabelaFaixas.CSV_ADULTO)),
		TabelaAtividadesMet.doCsv(getClass().getClassLoader().getResourceAsStream(TabelaAtividadesMet.CSV)));
	static final UUID ID=UUID.randomUUID();
	final Instant quando=ZonedDateTime.of(2026,10,31,12,0,0,0,ZoneId.of("America/Sao_Paulo")).toInstant();
	final FichaParaLista homem=new FichaParaLista(ID,ID,StatusVinculo.ATIVO,LocalDate.of(1997,4,1),Sexo.MASCULINO);

	SalvarAvaliacaoRequisicao req(NivelAtividade nivel,ObjetivoPeso objetivo,BigDecimal kgMes,EnergiaAvaliacao energia){
		var compl=new MedidasComplementaresAvaliacao(null,null,null,null,null,null,nivel,objetivo,kgMes,null);
		return new SalvarAvaliacaoRequisicao(null,quando,new BigDecimal("102.5"),new BigDecimal("1.93"),new PerimetrosAvaliacao(null,null,new BigDecimal("100"),null,null,null,null,null,null),
			new DobrasAvaliacao(new BigDecimal("20"),new BigDecimal("30"),new BigDecimal("25"),new BigDecimal("28"),new BigDecimal("32"),new BigDecimal("40"),new BigDecimal("24")),
			null,null,true,null,null,compl,null,energia);
	}
	EnergiaResultado energia(SalvarAvaliacaoRequisicao r){return calculo.calcular(r,homem,null,new BigDecimal("27.52")).energia();}

	@Test void semEscolhaENemNivelDeAtividadeNaoGravaEnergia(){
		assertThat(energia(req(null,null,null,null))).isNull();
	}
	@Test void padraoEFao2004ComFaixasDriEMinimoDeLipidio(){
		var e=energia(req(NivelAtividade.MODERADO,null,null,null));
		assertThat(e.metodo()).isEqualTo(MetodoGet.FAO_2004);
		assertThat(e.gebKcal()).isEqualByComparingTo("2235.54");assertThat(e.getKcal()).isEqualByComparingTo("3934.55");assertThat(e.vetKcal()).isEqualByComparingTo("3934.55");
		assertThat(e.kcalPorKg()).isEqualByComparingTo("38.39");
		assertThat(e.carboidrato().percentualMinimo()).isEqualByComparingTo("45");assertThat(e.carboidrato().percentualMaximo()).isEqualByComparingTo("65");
		assertThat(e.proteina().percentualMinimo()).isEqualByComparingTo("10");assertThat(e.lipidio().percentualMinimo()).isEqualByComparingTo("20");
		assertThat(e.lipidiosDislipidemico()).isNull();assertThat(e.motivosNulos()).isNull();
	}
	@Test void objetivoDePerdaTiraKcalDoVet(){
		var e=energia(req(NivelAtividade.MODERADO,ObjetivoPeso.PERDA,new BigDecimal("2"),null));
		assertThat(e.vetKcal()).isLessThan(e.getKcal());assertThat(e.getKcal()).isEqualByComparingTo("3934.55");
	}
	@Test void metodoEscolhidoMudaAFormulaEFaltaDeNivelViraMotivoNuncaZero(){
		var f85=energia(req(NivelAtividade.LEVE,null,null,new EnergiaAvaliacao(MetodoGet.FAO_1985,null,null,null,null,null,null,null,null)));
		assertThat(f85.metodo()).isEqualTo(MetodoGet.FAO_1985);assertThat(f85.gebKcal()).isEqualByComparingTo("2247.25");
		var rdi=energia(req(NivelAtividade.LEVE,null,null,new EnergiaAvaliacao(MetodoGet.RDI_2005,null,null,null,null,null,null,null,null)));
		assertThat(rdi.gebKcal()).isNull();assertThat(rdi.getKcal()).isNotNull();
		var sem=energia(req(null,null,null,new EnergiaAvaliacao(MetodoGet.IOM_2005,null,null,null,null,null,null,null,null)));
		assertThat(sem.gebKcal()).isNotNull();assertThat(sem.getKcal()).isNull();assertThat(sem.vetKcal()).isNull();assertThat(sem.proteina()).isNull();
		assertThat(sem.motivosNulos()).containsEntry("get","FALTA_NIVEL_ATIVIDADE").containsEntry("vet","FALTA_GET");
	}
	@Test void protocolosEDislipidemicoVoltamNasMetas(){
		var e=energia(req(NivelAtividade.LEVE,null,null,new EnergiaAvaliacao(null,null,null,ProtocoloCarboidrato.POR_KG,ProtocoloProteina.HIPERTROFIA,ProtocoloLipidio.MINIMO_25,true,new BigDecimal("230"),null)));
		assertThat(e.carboidrato().gramasMinimo()).isEqualByComparingTo("512.50");assertThat(e.proteina().gramasMaximo()).isEqualByComparingTo("184.50");
		assertThat(e.lipidio().percentualMinimo()).isEqualByComparingTo("25");
		assertThat(e.lipidiosDislipidemico().saturado().percentualMinimo()).isEqualByComparingTo("7");assertThat(e.colesterolMaximoMg()).isEqualByComparingTo("200");
	}
	@Test void metUsaARotinaEExigeMassaMagraDaAvaliacao(){
		var rotina=new RotinaMet(new BigDecimal("8"),new BigDecimal("8"),NivelAtividade.LEVE,List.of(new AtividadeDiaria("caminhada_3_2_km_h",new BigDecimal("60"))));
		var e=energia(req(null,null,null,new EnergiaAvaliacao(MetodoGet.MET,null,null,null,null,null,null,null,rotina)));
		assertThat(e.metodo()).isEqualTo(MetodoGet.MET);assertThat(e.gebKcal()).isNotNull();assertThat(e.getKcal()).isGreaterThan(e.gebKcal());
		var semRotina=energia(req(null,null,null,new EnergiaAvaliacao(MetodoGet.MET,null,null,null,null,null,null,null,null)));
		assertThat(semRotina.getKcal()).isNull();assertThat(semRotina.motivosNulos()).containsEntry("get","FALTA_ROTINA_MET");
	}
	@Test void atividadeDesconhecidaEHorasAcimaDe24SaoRejeitadas(){
		var desconhecida=new RotinaMet(new BigDecimal("8"),new BigDecimal("8"),NivelAtividade.LEVE,List.of(new AtividadeDiaria("inexistente",new BigDecimal("60"))));
		assertThatThrownBy(()->energia(req(null,null,null,new EnergiaAvaliacao(MetodoGet.MET,null,null,null,null,null,null,null,desconhecida)))).isInstanceOf(ExcecaoNegocio.class);
		var cheia=new RotinaMet(new BigDecimal("20"),new BigDecimal("8"),NivelAtividade.LEVE,List.of());
		assertThatThrownBy(()->energia(req(null,null,null,new EnergiaAvaliacao(MetodoGet.MET,null,null,null,null,null,null,null,cheia)))).isInstanceOf(ExcecaoNegocio.class);
	}
	@Test void faixaNaoSuportadaOuFichaIncompletaDaMotivoDaEnergia(){
		var jovem=new FichaParaLista(ID,ID,StatusVinculo.ATIVO,LocalDate.of(2012,1,1),Sexo.FEMININO);
		var e=calculo.calcular(req(NivelAtividade.LEVE,null,null,null),jovem,null,BigDecimal.ONE).energia();
		assertThat(e.getKcal()).isNull();assertThat(e.motivosNulos()).containsEntry("energia","FAIXA_ETARIA_NAO_SUPORTADA");
	}
	@Test void pacienteSoVeAEnergiaSeANutriCompartilhouNecessidades(){
		var r=req(NivelAtividade.MODERADO,null,null,new EnergiaAvaliacao(MetodoGet.FAO_2004,null,null,null,null,null,null,null,null));
		var c=calculo.calcular(r,homem,null,new BigDecimal("27.52"));
		java.util.function.Function<CompartilhamentoAvaliacao,AvaliacaoResposta> visto=comp->VisibilidadeAvaliacao.paraPaciente(new AvaliacaoResposta(ID,ID,ID,quando,r.pesoKg(),r.estaturaM(),BigDecimal.ONE,ClassificacaoImc.NORMAL,
			PerimetrosAvaliacao.VAZIO,DobrasAvaliacao.VAZIO,ComposicaoAvaliacao.VAZIO,null,true,1L,quando,quando,c.faixaEtaria(),c.protocolo(),r.complementares(),c.resultados(),comp,r.energia(),c.energia()));
		assertThat(visto.apply(null).energiaResultado().getKcal()).isEqualByComparingTo("3934.55");
		var escondida=visto.apply(new CompartilhamentoAvaliacao(null,null,false,null,null));
		assertThat(escondida.energia()).isNull();assertThat(escondida.energiaResultado()).isNull();
	}
}
