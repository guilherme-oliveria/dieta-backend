package com.br.dietamais.dietas;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.api.TotaisNutricionaisResposta;
import com.br.dietamais.dietas.dominio.AdequacaoDri;
import com.br.dietamais.dietas.dominio.AdequacaoDri.Contribuicao;
import com.br.dietamais.referencias.api.ValorDri;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class AdequacaoDriTest {
	static BigDecimal n(String v){return new BigDecimal(v);}
	@Test void somaSoQuemInformaEListaIncompletoSemVirarZero(){
		var a=new Contribuicao(Map.of("ca",n("100.004"),"fe",n("1")));
		var b=new Contribuicao(Map.of("ca",n("50.004")));
		var c=new Contribuicao(Map.of());
		var s=AdequacaoDri.somar(List.of(a,b,c));
		assertThat(s.micronutrientes()).containsOnlyKeys("ca","fe");
		assertThat(s.micronutrientes().get("ca")).isEqualByComparingTo("150.01");
		assertThat(s.micronutrientes().get("fe")).isEqualByComparingTo("1.00");
		assertThat(s.incompleto()).containsExactly("ca","fe");
		assertThat(AdequacaoDri.somar(List.of(a,b)).incompleto()).containsExactly("fe");
	}
	@Test void semNenhumMicronutrienteInformadoNaoDevolveMapa(){
		var s=AdequacaoDri.somar(List.of(new Contribuicao(Map.of())));
		assertThat(s.micronutrientes()).isNull();assertThat(s.incompleto()).isNull();
		assertThat(AdequacaoDri.somar(List.of()).micronutrientes()).isNull();
	}
	@Test void adequacaoLigaCodigosDaPorcaoAosDaDriEClassificaPorPercentual(){
		var dia=new TotaisNutricionaisResposta(n("1800"),n("200"),n("40.00"),n("60")).comMicronutrientes(
			new LinkedHashMap<>(Map.of("ca",n("1000.00"),"fe",n("30.00"),"agua",n("2500.00"),"vita",n("5.00"))),List.of());
		var metas=List.of(new ValorDri("proteina","Proteína","g/dia",n("50")),new ValorDri("calcio","Cálcio","mg/dia",n("1000")),new ValorDri("ferro","Ferro","mg/dia",n("8")),
			new ValorDri("agua","Água","l/dia",n("3.7")),new ValorDri("zinco","Zinco","mg/dia",n("11")));
		var r=AdequacaoDri.adequacao(dia,metas);
		assertThat(r).extracting("nutriente").containsExactly("proteina","calcio","ferro","agua");
		assertThat(r.get(0).percentual()).isEqualByComparingTo("80.00");assertThat(r.get(0).classe()).isEqualTo("ABAIXO");
		assertThat(r.get(1).classe()).isEqualTo("ADEQUADO");assertThat(r.get(1).percentual()).isEqualByComparingTo("100.00");
		assertThat(r.get(2).percentual()).isEqualByComparingTo("375.00");assertThat(r.get(2).classe()).isEqualTo("ACIMA");
		assertThat(r.get(3).total()).isEqualByComparingTo("2.5");assertThat(r.get(3).percentual()).isEqualByComparingTo("67.57");
	}
	@Test void limitesDe90E110SaoAdequados(){
		var dia=new TotaisNutricionaisResposta(n("1"),n("1"),n("45.00"),n("1")).comMicronutrientes(Map.of("ca",n("1100.00")),List.of());
		var r=AdequacaoDri.adequacao(dia,List.of(new ValorDri("proteina","Proteína","g/dia",n("50")),new ValorDri("calcio","Cálcio","mg/dia",n("1000"))));
		assertThat(r.get(0).classe()).isEqualTo("ADEQUADO");assertThat(r.get(1).classe()).isEqualTo("ADEQUADO");
	}
}
