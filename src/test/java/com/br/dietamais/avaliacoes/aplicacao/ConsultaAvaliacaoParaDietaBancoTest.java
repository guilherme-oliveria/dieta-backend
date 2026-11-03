package com.br.dietamais.avaliacoes.aplicacao;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.avaliacoes.api.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ConsultaAvaliacaoParaDietaBancoTest {
	static MetaMacro faixa(String gmin,String gmax){return new MetaMacro(null,null,new BigDecimal(gmin),gmax==null?null:new BigDecimal(gmax));}
	static EnergiaResultado resultado(String vet,MetaMacro carbo,MetaMacro ptn){
		return new EnergiaResultado(MetodoGet.FAO_2004,null,null,vet==null?null:new BigDecimal(vet),null,carbo,ptn,null,null,null,null,null);
	}

	@Test void metasUsamOMeioDaFaixaEOLipidioFechaOResto(){
		// carboidrato 225 a 325 g (meio 275 g = 1100 kcal = 55%), proteína 50 a 100 g (meio 75 g = 300 kcal = 15%) sobre 2000 kcal
		var m=ConsultaAvaliacaoParaDietaBanco.metas(resultado("2000",faixa("225","325"),faixa("50","100")));
		assertThat(m.vetKcal()).isEqualByComparingTo("2000");
		assertThat(m.carboidratoPct()).isEqualByComparingTo("55");assertThat(m.proteinaPct()).isEqualByComparingTo("15");assertThat(m.lipidioPct()).isEqualByComparingTo("30");
	}
	@Test void faixaSoComMinimoUsaOMinimo(){
		var m=ConsultaAvaliacaoParaDietaBanco.metas(resultado("1800",faixa("130",null),faixa("46",null)));
		assertThat(m.carboidratoPct()).isEqualByComparingTo("28.89");assertThat(m.proteinaPct()).isEqualByComparingTo("10.22");assertThat(m.lipidioPct()).isEqualByComparingTo("60.89");
	}
	@Test void semVetOuSemMacrosNaoHaMetas(){
		assertThat(ConsultaAvaliacaoParaDietaBanco.metas(null)).isNull();
		assertThat(ConsultaAvaliacaoParaDietaBanco.metas(resultado(null,faixa("1","2"),faixa("1","2")))).isNull();
		assertThat(ConsultaAvaliacaoParaDietaBanco.metas(resultado("2000",null,faixa("1","2")))).isNull();
	}
}
