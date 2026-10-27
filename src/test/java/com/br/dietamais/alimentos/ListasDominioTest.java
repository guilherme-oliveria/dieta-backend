package com.br.dietamais.alimentos;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.alimentos.dominio.*;
import com.br.dietamais.comum.api.ExcecaoNegocio;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class ListasDominioTest {
	@Test void versaoForteNaoAceitaWildcardFracaListaOuOverflow(){
		assertThat(VersaoLista.ler("\"0\"")).isZero();
		for(String valor:new String[]{null,"","1","*","W/\"1\"","\"1\",\"2\"","\"-1\"","\"9223372036854775808\""})assertThatThrownBy(()->VersaoLista.ler(valor)).isInstanceOf(ExcecaoNegocio.class);
	}
	@Test void diferencasSaoConjuntosDisjuntosComReferenciasObrigatorias(){
		UUID id=UUID.randomUUID();
		assertThatThrownBy(()->new DadosAjusteLista(List.of(id,id),List.of())).isInstanceOf(ExcecaoNegocio.class);
		assertThatThrownBy(()->new DadosAjusteLista(List.of(id),List.of(id))).isInstanceOf(ExcecaoNegocio.class);
		assertThatThrownBy(()->new DadosAjusteLista(null,List.of())).isInstanceOf(ExcecaoNegocio.class);
		assertThatThrownBy(()->new DadosAjusteLista(Arrays.asList((UUID)null),List.of())).isInstanceOf(ExcecaoNegocio.class);
		assertThat(new DadosAjusteLista(List.of(),List.of()).adicionados()).isEmpty();
	}
	@Test void alimentoPreservaMedidaERejeitaQuantidadeQueNaoCabeNoBanco(){
		var dados=new DadosAlimento(UUID.randomUUID()," Nome fictício "," 1 unidade ",new BigDecimal("10.1250"),true);
		assertThat(dados.nome()).isEqualTo("Nome fictício");assertThat(dados.medidaCaseira()).isEqualTo("1 unidade");
		for(var peso:List.of(BigDecimal.ZERO,new BigDecimal("-1"),new BigDecimal("0.00001"),new BigDecimal("100000000")))assertThatThrownBy(()->new DadosAlimento(UUID.randomUUID(),"Fictício","1 unidade",peso,true)).isInstanceOf(ExcecaoNegocio.class);
	}
}
