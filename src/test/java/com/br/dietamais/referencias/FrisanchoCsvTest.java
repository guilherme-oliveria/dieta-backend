package com.br.dietamais.referencias;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Confere o CSV de Frisancho (BE-54b) contra a aba outras da planilha de 0 a 9 anos. */
class FrisanchoCsvTest {
	List<String[]> ler() throws Exception {
		try(var in=getClass().getResourceAsStream("/db/changelog/dados/referencia-frisancho.csv")){
			var l=new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\n");var r=new ArrayList<String[]>();
			for(int i=1;i<l.length;i++)r.add(l[i].split(";",-1));
			return r;
		}
	}
	String[] achar(List<String[]> l,String ind,String sexo,int idade){return l.stream().filter(c->c[0].equals(ind)&&c[1].equals(sexo)&&c[2].equals(String.valueOf(idade))).findFirst().orElseThrow();}

	@Test void tem80LinhasDe2A9AnosPorIndicadorESexo() throws Exception {
		var l=ler();
		assertThat(l).hasSize(80);
		assertThat(l.stream().map(c->c[0]+c[1]+c[2]).distinct()).hasSize(80);
		assertThat(l).allSatisfy(c->assertThat(Integer.parseInt(c[2])).isBetween(2,9));
	}
	@Test void valoresConferidosComAAba() throws Exception {
		var l=ler();
		var cb=achar(l,"CB","feminino",2);assertThat(cb[5]).isEqualTo("16");assertThat(cb[3]).isEqualTo("NULL");
		assertThat(achar(l,"DCT","masculino",9)[5]).isEqualTo("10");
		var ambc=achar(l,"AMBC","masculino",4);assertThat(List.of(ambc[3],ambc[4],ambc[5],ambc[6])).containsExactly("1297","1264","1579","1926");
		var agb=achar(l,"AGB","feminino",9);assertThat(List.of(agb[3],agb[4],agb[5],agb[6])).containsExactly("642","690","1219","2171");
	}
	/** Inconsistência da planilha (G-16): AMBC masculino de 4 anos tem o percentil 5 (1297) acima do 10 (1264). Mantido como está. */
	@Test void percentisDeAmbcEAgbCrescemExcetoOCasoConhecidoDaPlanilha() throws Exception {
		var fora=ler().stream().filter(c->c[0].equals("AMBC")||c[0].equals("AGB")).filter(c->!(Double.parseDouble(c[3])<=Double.parseDouble(c[4])&&Double.parseDouble(c[4])<=Double.parseDouble(c[5])&&Double.parseDouble(c[5])<=Double.parseDouble(c[6]))).toList();
		assertThat(fora).hasSize(1);assertThat(fora.getFirst()[0]+fora.getFirst()[1]+fora.getFirst()[2]).isEqualTo("AMBCmasculino4");
	}
}
