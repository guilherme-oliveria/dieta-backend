package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.aplicacao.CalculoAvaliacao;
import com.br.dietamais.avaliacoes.dominio.VisibilidadeAvaliacao;
import com.br.dietamais.pacientes.api.*;
import com.br.dietamais.referencias.api.TabelaFaixas;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CalculoAvaliacaoTest {
	final CalculoAvaliacao calculo=new CalculoAvaliacao(TabelaFaixas.doCsv(getClass().getClassLoader().getResourceAsStream(TabelaFaixas.CSV_ADULTO)),com.br.dietamais.referencias.api.TabelaAtividadesMet.doCsv(getClass().getClassLoader().getResourceAsStream(com.br.dietamais.referencias.api.TabelaAtividadesMet.CSV)));
	static final UUID ID=UUID.randomUUID();
	SalvarAvaliacaoRequisicao requisicao(Instant quando,ComposicaoAvaliacao c){
		return new SalvarAvaliacaoRequisicao(null,quando,new BigDecimal("102.5"),new BigDecimal("1.93"),new PerimetrosAvaliacao(null,null,new BigDecimal("100"),null,null,null,null,null,null),
			new DobrasAvaliacao(new BigDecimal("20"),new BigDecimal("30"),new BigDecimal("25"),new BigDecimal("28"),new BigDecimal("32"),new BigDecimal("40"),new BigDecimal("24")),c,null,true,null,null,null,null);
	}
	FichaParaLista ficha(LocalDate nascimento,Sexo sexo){return new FichaParaLista(ID,ID,StatusVinculo.ATIVO,nascimento,sexo);}
	final Instant quando=ZonedDateTime.of(2026,10,31,12,0,0,0,ZoneId.of("America/Sao_Paulo")).toInstant();

	@Test void faixaEtariaPelaIdade(){
		assertThat(CalculoAvaliacao.faixa(9)).isEqualTo(FaixaEtaria.CRIANCA);assertThat(CalculoAvaliacao.faixa(10)).isEqualTo(FaixaEtaria.ADOLESCENTE);
		assertThat(CalculoAvaliacao.faixa(17)).isEqualTo(FaixaEtaria.ADOLESCENTE);assertThat(CalculoAvaliacao.faixa(18)).isEqualTo(FaixaEtaria.ADULTO);
		assertThat(CalculoAvaliacao.faixa(59)).isEqualTo(FaixaEtaria.ADULTO);assertThat(CalculoAvaliacao.faixa(60)).isEqualTo(FaixaEtaria.IDOSO);
	}
	@Test void adultoUsaProtocoloPadraoEVersaoDoServidor(){
		var c=calculo.calcular(requisicao(quando,null),ficha(LocalDate.of(1997,4,1),Sexo.MASCULINO),null,new BigDecimal("27.52"));
		assertThat(c.faixaEtaria()).isEqualTo(FaixaEtaria.ADULTO);assertThat(c.protocolo()).isEqualTo(new ProtocoloAvaliacao(ProtocoloDobras.SETE,ProtocoloMassa.MAGRO,"adulto-1.0.0"));
		assertThat(c.resultados().idadeAnos()).isEqualTo(29);assertThat(c.resultados().percentualGordura()).isEqualByComparingTo("26.48");
	}
	@Test void pedidoDeProtocoloIgnoraAVersaoEnviada(){
		var c=calculo.calcular(requisicao(quando,null),ficha(LocalDate.of(1997,4,1),Sexo.MASCULINO),new ProtocoloAvaliacao(ProtocoloDobras.TRES,ProtocoloMassa.MUSCULAR,"x"),BigDecimal.ONE);
		assertThat(c.protocolo().versaoReferencia()).isEqualTo("adulto-1.0.0");assertThat(c.protocolo().dobras()).isEqualTo(ProtocoloDobras.TRES);
	}
	@Test void semDadosDaFichaFicaNuloComMotivoEAdolescenteAindaNaoCalcula(){
		assertThat(calculo.calcular(requisicao(quando,null),ficha(null,Sexo.MASCULINO),null,BigDecimal.ONE).resultados().motivosNulos()).containsEntry("resultados","FALTA_NASCIMENTO");
		assertThat(calculo.calcular(requisicao(quando,null),ficha(LocalDate.of(1997,4,1),Sexo.NAO_INFORMADO),null,BigDecimal.ONE).resultados().motivosNulos()).containsEntry("resultados","FALTA_SEXO");
		var jovem=calculo.calcular(requisicao(quando,null),ficha(LocalDate.of(2012,1,1),Sexo.FEMININO),null,BigDecimal.ONE);
		assertThat(jovem.faixaEtaria()).isEqualTo(FaixaEtaria.ADOLESCENTE);assertThat(jovem.resultados().motivosNulos()).containsEntry("resultados","FAIXA_ETARIA_NAO_SUPORTADA");
	}
	@Test void pacienteNaoVeSecaoNaoCompartilhada(){
		var c=calculo.calcular(requisicao(quando,null),ficha(LocalDate.of(1997,4,1),Sexo.MASCULINO),null,new BigDecimal("27.52"));
		var comp=new ComposicaoAvaliacao(new BigDecimal("26.48"),new BigDecimal("27.15"),new BigDecimal("75.35"));
		var resposta=new AvaliacaoResposta(ID,ID,ID,quando,new BigDecimal("102.5"),new BigDecimal("1.93"),new BigDecimal("27.52"),ClassificacaoImc.SOBREPESO,PerimetrosAvaliacao.VAZIO,DobrasAvaliacao.VAZIO,comp,"interno",true,1L,quando,quando,
			c.faixaEtaria(),c.protocolo(),null,c.resultados(),new CompartilhamentoAvaliacao(null,false,null,false,null));
		var visto=VisibilidadeAvaliacao.paraPaciente(resposta);
		assertThat(visto.composicao()).isEqualTo(ComposicaoAvaliacao.VAZIO);assertThat(visto.parecer()).isNull();assertThat(visto.resultados().percentualGordura()).isNull();assertThat(visto.resultados().textoResumo()).isNull();
		assertThat(visto.resultados().classificacaoImc()).isEqualTo(ClasseImc.SP);assertThat(visto.resultados().motivosNulos()).isNull();assertThat(visto.pesoKg()).isEqualByComparingTo("102.5");
		var padrao=VisibilidadeAvaliacao.paraPaciente(new AvaliacaoResposta(ID,ID,ID,quando,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,ClassificacaoImc.NORMAL,PerimetrosAvaliacao.VAZIO,DobrasAvaliacao.VAZIO,comp,"aberto",true,1L,quando,quando,null,null,null,null,null));
		assertThat(padrao.parecer()).isEqualTo("aberto");assertThat(padrao.composicao()).isEqualTo(comp);
	}
}
