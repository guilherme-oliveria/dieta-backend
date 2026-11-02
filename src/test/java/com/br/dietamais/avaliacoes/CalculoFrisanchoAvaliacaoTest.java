package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.avaliacoes.aplicacao.CalculoFrisanchoAvaliacao;
import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CalculoFrisanchoAvaliacaoTest {
	private static BigDecimal n(String s){return new BigDecimal(s);}
	private final ConsultaFrisancho referencias=(i,s,a)->{
		if(a<2||a>9)return Optional.empty();
		if(i.equals("AMBC"))return Optional.of(new PercentisFrisancho(i,s,a,n("1297"),n("1264"),n("1579"),n("1926")));
		if(i.equals("AGB"))return Optional.of(new PercentisFrisancho(i,s,a,n("428"),n("494"),n("722"),n("989")));
		return Optional.of(new PercentisFrisancho(i,s,a,null,null,n("20"),null));
	};
	private final CalculoFrisanchoAvaliacao calculo=new CalculoFrisanchoAvaliacao(referencias);

	@Test void adequacaoUsaMedianaDeCbSemInventarCmb(){
		var r=calculo.calcular("CB","masculino",4,n("20"));
		assertThat(r.adequacaoPercentual()).isEqualByComparingTo("100");assertThat(r.classificacao()).isEqualTo("A");
		assertThat(r.unidade()).isEqualTo("cm");assertThat(r.percentil()).isNull();assertThat(r.motivo()).isNull();
	}
	@Test void g16PreservaFonteMasOmiteClassificacaoEPosicao(){
		var r=calculo.calcular("AMBC","masculino",4,n("1300"));
		assertThat(r.classificacao()).isNull();assertThat(r.percentil()).isNull();assertThat(r.motivo()).isEqualTo("REFERENCIA_INCONSISTENTE_G16");
		assertThat(r.p5()).isEqualByComparingTo("1297");assertThat(r.p10()).isEqualByComparingTo("1264");assertThat(r.valor()).isEqualByComparingTo("1300");
	}
	@Test void areaUsaCortesEUnidadeDaCalculadora(){
		var r=calculo.calcular("AGB","masculino",4,n("500"));
		assertThat(r.classificacao()).isEqualTo("P10_A_P90");assertThat(r.unidade()).isEqualTo("mm²");
		assertThat(r.percentil().inferior()).isEqualByComparingTo("10");assertThat(r.percentil().superior()).isEqualByComparingTo("50");
		assertThat(r.adequacaoPercentual()).isNull();
	}
	@Test void foraDaTabelaNaoInventaReferencia(){
		assertThat(calculo.calcular("CB","masculino",1,n("20")).motivo()).isEqualTo("REFERENCIA_INDISPONIVEL");
		assertThat(calculo.calcular("CB","masculino",10,n("20")).motivo()).isEqualTo("REFERENCIA_INDISPONIVEL");
		assertThat(calculo.calcular("CB",null,4,n("20")).motivo()).isEqualTo("FALTA_SEXO");
		assertThat(calculo.calcular("CB","masculino",4,null).motivo()).isEqualTo("FALTA_MEDIDA");
	}
}
