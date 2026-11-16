package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.aplicacao.*;
import com.br.dietamais.pacientes.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CalculoAvaliacaoInfantilTest {
	private final CalculoAvaliacaoInfantil calculo=new CalculoAvaliacaoInfantil(new CalculoCurvasInfantis((i,s,m)->Optional.empty()),new CalculoFrisanchoAvaliacao((i,s,a)->Optional.empty()));
	private static final UUID ID=UUID.randomUUID();
	private static final Instant DIA=Instant.parse("2026-11-02T12:00:00Z");
	private FichaParaLista ficha(LocalDate nascimento,Sexo sexo){return new FichaParaLista(ID,ID,StatusVinculo.ATIVO,nascimento,sexo);}
	private SalvarAvaliacaoRequisicao pedido(String peso,String altura,DadosInfantisAvaliacao infantil){
		return new SalvarAvaliacaoRequisicao(null,DIA,new BigDecimal(peso),new BigDecimal(altura),null,null,null,null,true,null,null,null,null,null,null,infantil);
	}
	@Test void lactenteUsaMesesCompletosERdiSemAtividade(){
		var r=calculo.calcular(pedido("5","0.6",new DadosInfantisAvaliacao(2500,null,null,null)),ficha(LocalDate.of(2026,8,2),Sexo.FEMININO));
		assertThat(r.idadeMeses()).isEqualTo(3);assertThat(r.idadeTexto()).isEqualTo("3 meses");
		assertThat(r.classificacaoPesoNascer()).isEqualTo("BAIXO");assertThat(r.energia().getRdiKcal()).isEqualByComparingTo("520");
		assertThat(r.energia().getIomKcal()).isNull();assertThat(r.energia().motivosNulos()).containsEntry("getIomKcal","IDADE_FORA_DA_FORMULA");
	}
	@Test void aniversarioDeDezAnosMudaCurvasEComposicao(){
		var r=calculo.calcular(pedido("30","1.3",null),ficha(LocalDate.of(2016,11,2),Sexo.MASCULINO));
		assertThat(r.idadeMeses()).isEqualTo(120);assertThat(r.curvas()).extracting(CurvaCrescimentoAvaliacao::indicador).containsExactly("IMC_IDADE");
		assertThat(r.composicao().motivosNulos()).containsEntry("percentualGordura","FALTA_DOBRAS");
		assertThat(r.energia().motivosNulos()).containsEntry("getRdiKcal","FALTA_FATOR_ATIVIDADE");
	}
	@Test void semSexoPreservaIdadeEPesoNascerSemProduzirEnergia(){
		var r=calculo.calcular(pedido("10","0.8",new DadosInfantisAvaliacao(3200,ModoCurva.ESCORE_Z,null,null)),ficha(LocalDate.of(2025,11,2),Sexo.NAO_INFORMADO));
		assertThat(r.idadeMeses()).isEqualTo(12);assertThat(r.classificacaoPesoNascer()).isEqualTo("NORMAL");
		assertThat(r.curvas()).allSatisfy(c->assertThat(c.motivo()).isEqualTo("FALTA_SEXO"));
		assertThat(r.energia().getRdiKcal()).isNull();assertThat(r.energia().motivosNulos()).containsEntry("energia","FALTA_SEXO");
	}
	@Test void adultoOuNascimentoInvalidoNaoUsamCalculadorasInfantis(){
		assertThat(calculo.calcular(pedido("70","1.7",null),ficha(LocalDate.of(2008,11,2),Sexo.FEMININO))).isNull();
		assertThat(calculo.calcular(pedido("3","0.5",null),ficha(LocalDate.of(2026,11,3),Sexo.FEMININO))).isNull();
		assertThat(calculo.calcular(pedido("3","0.5",null),ficha(null,Sexo.FEMININO))).isNull();
	}
	@Test void integraSlaughterEClassificacaoSemAplicarFormulasAdultas(){
		var r=new SalvarAvaliacaoRequisicao(null,DIA,new BigDecimal("30"),new BigDecimal("1.3"),null,
			new DobrasAvaliacao(BigDecimal.TEN,BigDecimal.TEN,null,null,null,null,null),null,null,true,null,null,null,null,null,null,new DadosInfantisAvaliacao(null,null,EtniaSlaughter.BRANCA,BigDecimal.ONE));
		var resultado=calculo.calcular(r,ficha(LocalDate.of(2013,11,2),Sexo.MASCULINO));
		assertThat(resultado.composicao().percentualGordura()).isEqualByComparingTo("17.60");
		assertThat(resultado.composicao().classificacaoPercentualGordura()).isEqualTo("ADEQ");
		assertThat(resultado.composicao().pesoGordoKg()).isEqualByComparingTo("5.28");
		assertThat(resultado.composicao().pesoMagroKg()).isEqualByComparingTo("24.72");
		assertThat(resultado.energia().getIomKcal()).isNotNull();assertThat(resultado.energia().getRdiKcal()).isNotNull();
	}
}
