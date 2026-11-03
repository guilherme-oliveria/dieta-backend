package com.br.dietamais.referencias;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Confere o CSV da TACO (BE-47a) sem banco: 597 alimentos, números únicos e energia coerente com os macronutrientes. */
class TacoCsvTest {
	List<Map<String,String>> ler() throws Exception {
		try(var in=getClass().getResourceAsStream("/db/changelog/dados/referencia-taco.csv")){
			var linhas=new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\n");
			var cab=linhas[0].split(";",-1);var r=new ArrayList<Map<String,String>>();
			for(int i=1;i<linhas.length;i++){var c=linhas[i].split(";",-1);var m=new HashMap<String,String>();for(int j=0;j<cab.length;j++)m.put(cab[j],c[j]);r.add(m);}
			return r;
		}
	}
	double v(Map<String,String> m,String c){var s=m.get(c);return s==null||s.isEmpty()?0:Double.parseDouble(s);}

	@Test void tem597AlimentosComNumeroUnicoENomeECategoria() throws Exception {
		var r=ler();
		assertThat(r).hasSize(597);
		assertThat(r.stream().map(m->m.get("numero")).distinct()).hasSize(597);
		assertThat(r).allSatisfy(m->{assertThat(m.get("nome")).isNotBlank();assertThat(m.get("categoria")).isNotBlank();});
		assertThat(r.stream().map(m->m.get("categoria")).distinct()).hasSize(15);
	}
	@Test void energiaBateComOsMacronutrientesExcetoAlcoolEFermento() throws Exception {
		var fora=new ArrayList<String>();
		for(var m:ler()){
			double k=v(m,"energia_kcal"),est=4*v(m,"proteina_g")+9*v(m,"lipideos_g")+4*v(m,"carboidrato_g");
			if(k>0&&Math.abs(est-k)>Math.max(25,0.25*k))fora.add(m.get("numero"));
		}
		assertThat(fora).containsExactlyInAnyOrder("472","474","513");
	}
	@Test void arrozIntegralCozidoBateComAFonte() throws Exception {
		var m=ler().get(0);
		assertThat(m.get("nome")).isEqualTo("Arroz, integral, cozido");
		assertThat(v(m,"energia_kcal")).isEqualTo(124);assertThat(v(m,"proteina_g")).isEqualTo(2.6);assertThat(v(m,"carboidrato_g")).isEqualTo(25.8);
		assertThat(m.get("colesterol_mg")).isEmpty();assertThat(m.get("traco")).contains("riboflavina_mg");
	}
}
