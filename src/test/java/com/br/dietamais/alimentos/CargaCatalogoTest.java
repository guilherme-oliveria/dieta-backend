package com.br.dietamais.alimentos;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.junit.jupiter.api.Test;

class CargaCatalogoTest {
	@Test void dadosGenericosMantemGruposMedidasEReferenciasSemInventarMacros() throws Exception {
		var grupos=linhas("grupos-alimentos.csv");var alimentos=linhas("alimentos.csv");
		assertThat(grupos).hasSize(12);assertThat(alimentos).hasSize(482);
		var ids=new HashSet<UUID>();var kcal=new HashMap<String,BigDecimal>();
		for(var g:grupos){assertThat(g).hasSize(6);assertThat(ids.add(UUID.fromString(g[0]))).isTrue();assertThat(new BigDecimal(g[2])).isPositive();kcal.put(g[1],new BigDecimal(g[2]));}
		assertThat(kcal).containsEntry("Pão",new BigDecimal("150")).containsEntry("Carne ou ovo",new BigDecimal("200"))
			.containsEntry("Arroz",new BigDecimal("80")).containsEntry("Fruta",new BigDecimal("70"))
			.containsEntry("Leite",new BigDecimal("55")).containsEntry("Feijão",new BigDecimal("45"));
		var alimentosIds=new HashSet<UUID>();
		for(var a:alimentos){assertThat(a).hasSize(6);assertThat(alimentosIds.add(UUID.fromString(a[0]))).isTrue();assertThat(ids).contains(UUID.fromString(a[1]));assertThat(a[2]).isNotBlank().hasSizeLessThanOrEqualTo(150);assertThat(a[3]).isNotBlank();assertThat(new BigDecimal(a[4])).isPositive();assertThat(a[5]).isEqualTo("true");}
		assertThat(alimentos.stream().map(a->a[2])).contains("Boi - Assada","Leites - Com café e açúcar");
	}
	@Test void estimativasSaoRastreaveisNormalizadasPorEnergiaSemConfundirCemGramasComPorcao() throws Exception {
		var fontes=linhas("fontes-macros-grupos.csv");assertThat(fontes).hasSize(12);
		var porId=new HashMap<String,String[]>();for(var fonte:fontes){assertThat(fonte).hasSize(15);assertThat(porId.put(fonte[0],fonte)).isNull();assertThat(fonte[2]).startsWith("https://nepa.unicamp.br/");assertThat(fonte[4]).isEqualTo("2026-10-27");assertThat(fonte[14]).isEqualTo("true");}
		for(var grupo:linhas("grupos-alimentos.csv")){
			var fonte=porId.get(grupo[0]);assertThat(fonte).isNotNull();
			for(int i=0;i<3;i++){
				var estimado=new BigDecimal(fonte[9+i]).multiply(new BigDecimal(grupo[2])).divide(new BigDecimal(fonte[8]),1,RoundingMode.HALF_UP);
				assertThat(new BigDecimal(grupo[3+i])).isEqualByComparingTo(estimado).isNotNegative();
			}
		}
		var sopa=fontes.stream().filter(f->f[13].equals("proxy_composto_energia_v1")).findFirst().orElseThrow();
		assertThat(sopa[7]).isEqualTo("300");assertThat(sopa[8]).isEqualTo("364");
		for(int coluna:new int[]{7,8,9,10,11}){
			BigDecimal total=BigDecimal.ZERO;
			for(var f:fontes)if(Set.of("3","488","116").contains(f[5]))total=total.add(new BigDecimal(f[coluna]));
			assertThat(new BigDecimal(sopa[coluna])).isEqualByComparingTo(total);
		}
	}
	List<String[]> linhas(String arquivo) throws Exception {
		try(var input=getClass().getResourceAsStream("/db/changelog/dados/"+arquivo)){
			assertThat(input).isNotNull();return new String(input.readAllBytes(),StandardCharsets.UTF_8).lines().skip(1).map(l->l.split(";",-1)).toList();
		}
	}
}
