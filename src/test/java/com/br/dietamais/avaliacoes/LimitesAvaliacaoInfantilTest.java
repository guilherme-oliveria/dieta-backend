package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.dominio.LimitesAvaliacao;
import com.br.dietamais.comum.api.ExcecaoNegocio;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LimitesAvaliacaoInfantilTest {
	private SalvarAvaliacaoRequisicao pedido(String peso,String altura){
		return new SalvarAvaliacaoRequisicao(null,Instant.parse("2026-11-02T12:00:00Z"),new BigDecimal(peso),new BigDecimal(altura),null,null,null,null,true,null,null,null,null);
	}
	@Test void aceitaLactenteEEstaturaComPrecisaoDeMilimetro(){
		assertThatCode(()->LimitesAvaliacao.validar(pedido("3.25","0.485"),true)).doesNotThrowAnyException();
	}
	@Test void mantemLimitesAnterioresParaAdultos(){
		assertThatThrownBy(()->LimitesAvaliacao.validar(pedido("3.25","0.485"),false)).isInstanceOf(ExcecaoNegocio.class);
		assertThatThrownBy(()->LimitesAvaliacao.validar(pedido("70","1.735"),false)).isInstanceOf(ExcecaoNegocio.class);
	}
	@Test void rejeitaZeroNegativoEPrecisaoExcessivaMesmoParaCrianca(){
		assertThatThrownBy(()->LimitesAvaliacao.validar(pedido("0","0.485"),true)).isInstanceOf(ExcecaoNegocio.class);
		assertThatThrownBy(()->LimitesAvaliacao.validar(pedido("3","-0.485"),true)).isInstanceOf(ExcecaoNegocio.class);
		assertThatThrownBy(()->LimitesAvaliacao.validar(pedido("3","0.4851"),true)).isInstanceOf(ExcecaoNegocio.class);
	}
}
