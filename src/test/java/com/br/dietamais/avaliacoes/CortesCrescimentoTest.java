package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.avaliacoes.dominio.infantil.CortesCrescimento;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CortesCrescimentoTest {
	private static List<BigDecimal> numeros(String... valores){return java.util.Arrays.stream(valores).map(BigDecimal::new).toList();}
	private static final List<BigDecimal> POSICOES=numeros("0.1","3","10","15","50","85","97","99");
	private static final List<BigDecimal> CORTES=numeros("10","12","14","16","18","20","22","24");

	@Test void naoInterpolaEntreCortes(){
		var intervalo=CortesCrescimento.localizar(new BigDecimal("19"),CORTES,POSICOES).orElseThrow();
		assertThat(intervalo.inferior()).isEqualByComparingTo("50");
		assertThat(intervalo.superior()).isEqualByComparingTo("85");
		assertThat(intervalo.exato()).isFalse();
	}
	@Test void igualdadeNumericaIndependeDaEscala(){
		var intervalo=CortesCrescimento.localizar(new BigDecimal("18.00"),CORTES,POSICOES).orElseThrow();
		assertThat(intervalo.inferior()).isEqualByComparingTo("50");
		assertThat(intervalo.superior()).isEqualByComparingTo("50");
		assertThat(intervalo.exato()).isTrue();
	}
	@Test void extremosFicamAbertosSemExtrapolacao(){
		var abaixo=CortesCrescimento.localizar(BigDecimal.ONE,CORTES,POSICOES).orElseThrow();
		assertThat(abaixo.inferior()).isNull();assertThat(abaixo.superior()).isEqualByComparingTo("0.1");
		var acima=CortesCrescimento.localizar(new BigDecimal("25"),CORTES,POSICOES).orElseThrow();
		assertThat(acima.inferior()).isEqualByComparingTo("99");assertThat(acima.superior()).isNull();
	}
	@Test void escoreZPodeSerNegativo(){
		var intervalo=CortesCrescimento.localizar(new BigDecimal("11"),numeros("10","12","14"),numeros("-3","-2","-1")).orElseThrow();
		assertThat(intervalo.inferior()).isEqualByComparingTo("-3");assertThat(intervalo.superior()).isEqualByComparingTo("-2");
	}
	@Test void rejeitaFonteInconsistenteSemOrdenarOuCorrigir(){
		assertThat(CortesCrescimento.localizar(BigDecimal.TEN,numeros("12","10","14"),numeros("5","10","90"))).isEmpty();
		assertThat(CortesCrescimento.localizar(BigDecimal.TEN,numeros("10","10","14"),numeros("5","10","90"))).isEmpty();
		assertThat(CortesCrescimento.localizar(BigDecimal.TEN,numeros("10","12","14"),numeros("10","5","90"))).isEmpty();
	}
	@Test void fonteOuMedidaIncompletaNaoProduzPosicao(){
		assertThat(CortesCrescimento.localizar(null,CORTES,POSICOES)).isEmpty();
		assertThat(CortesCrescimento.localizar(BigDecimal.TEN,null,POSICOES)).isEmpty();
		assertThat(CortesCrescimento.localizar(BigDecimal.TEN,List.of(),List.of())).isEmpty();
		assertThat(CortesCrescimento.localizar(BigDecimal.TEN,CORTES,numeros("3"))).isEmpty();
		assertThat(CortesCrescimento.localizar(BigDecimal.TEN,java.util.Arrays.asList(BigDecimal.ONE,null),numeros("3","10"))).isEmpty();
	}
}
