package com.br.dietamais.referencias;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Confere os CSVs de porções (BE-47b) contra a aba "Alimentos" da planilha "Dieta sem restrições". */
class PorcoesCsvTest {
	List<String[]> ler(String nome) throws Exception {
		try(var in=getClass().getResourceAsStream("/db/changelog/dados/"+nome)){
			var l=new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\n");var r=new ArrayList<String[]>();
			for(int i=1;i<l.length;i++)r.add(l[i].split(";",-1));
			return r;
		}
	}
	static String grupo(int n){return String.format("019a0000-0000-7000-8000-%012d",n);}

	@Test void tem31PorcoesEmQuinzeGrupos() throws Exception {
		var p=ler("grupo-porcao.csv");
		assertThat(p).hasSize(31);
		assertThat(p.stream().map(c->c[1]).distinct()).hasSize(15);
		assertThat(p.stream().map(c->c[1]+c[2]).distinct()).hasSize(31);
	}
	@Test void quantidadeDePorcoesPorGrupoComoNaPlanilha() throws Exception {
		var p=ler("grupo-porcao.csv");
		java.util.function.IntFunction<Long> n=g->p.stream().filter(c->c[1].equals(grupo(g))).count();
		assertThat(List.of(n.apply(1),n.apply(2),n.apply(3),n.apply(4),n.apply(5),n.apply(6),n.apply(7),n.apply(8),n.apply(9),n.apply(10),n.apply(11),n.apply(12),n.apply(13),n.apply(14),n.apply(15)))
			.containsExactly(2L,2L,2L,4L,3L,3L,1L,2L,2L,2L,3L,1L,2L,1L,1L);
	}
	@Test void valoresConferidosComALinhaMediaDaPlanilha() throws Exception {
		var p=ler("grupo-porcao.csv");var nut=ler("grupo-porcao-nutriente.csv");
		var pao=p.stream().filter(c->c[1].equals(grupo(1))&&c[2].equals("1")).findFirst().orElseThrow();
		assertThat(Double.parseDouble(pao[3])).isEqualTo(70.19);
		assertThat(pao[6]).isEqualTo("½ unidade média de pão francês");
		var gli=nut.stream().filter(c->c[0].equals(pao[0])&&c[1].equals("gli")).findFirst().orElseThrow();
		assertThat(Double.parseDouble(gli[2])).isEqualTo(13.2060);
		var arroz4=p.stream().filter(c->c[1].equals(grupo(4))&&c[2].equals("4")).findFirst().orElseThrow();
		assertThat(arroz4[7]).isEqualTo("true");
		assertThat(Double.parseDouble(arroz4[3])).isEqualTo(147.24);
	}
	@Test void energiaDaPorcaoBateComAMediaDaEnergiaENutrientesNaoTemValorNegativo() throws Exception {
		var p=ler("grupo-porcao.csv");var nut=ler("grupo-porcao-nutriente.csv");
		for(var c:p){
			var e=nut.stream().filter(x->x[0].equals(c[0])&&x[1].equals("energia")).findFirst().orElseThrow();
			assertThat(Math.abs(Double.parseDouble(e[2])-Double.parseDouble(c[3]))).isLessThanOrEqualTo(0.01);
		}
		assertThat(nut).allSatisfy(c->assertThat(Double.parseDouble(c[2])).isGreaterThanOrEqualTo(0));
		assertThat(nut.stream().map(c->c[1]).distinct().count()).isLessThanOrEqualTo(34);
	}
}
