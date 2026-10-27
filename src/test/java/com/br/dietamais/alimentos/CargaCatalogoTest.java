package com.br.dietamais.alimentos;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class CargaCatalogoTest {
	@Test void dadosGenericosMantemGruposMedidasEReferenciasSemInventarMacros() throws Exception {
		var grupos=linhas("grupos-alimentos.csv");var alimentos=linhas("alimentos.csv");
		assertThat(grupos).hasSize(12);assertThat(alimentos).hasSize(482);
		var ids=new HashSet<UUID>();var kcal=new HashMap<String,BigDecimal>();
		for(var g:grupos){assertThat(g).hasSize(3);assertThat(ids.add(UUID.fromString(g[0]))).isTrue();assertThat(new BigDecimal(g[2])).isPositive();kcal.put(g[1],new BigDecimal(g[2]));}
		assertThat(kcal).containsEntry("Pão",new BigDecimal("150")).containsEntry("Carne ou ovo",new BigDecimal("200"))
			.containsEntry("Arroz",new BigDecimal("80")).containsEntry("Fruta",new BigDecimal("70"))
			.containsEntry("Leite",new BigDecimal("55")).containsEntry("Feijão",new BigDecimal("45"));
		var alimentosIds=new HashSet<UUID>();
		for(var a:alimentos){assertThat(a).hasSize(6);assertThat(alimentosIds.add(UUID.fromString(a[0]))).isTrue();assertThat(ids).contains(UUID.fromString(a[1]));assertThat(a[2]).isNotBlank().hasSizeLessThanOrEqualTo(150);assertThat(a[3]).isNotBlank();assertThat(new BigDecimal(a[4])).isPositive();assertThat(a[5]).isEqualTo("true");}
		assertThat(alimentos.stream().map(a->a[2])).contains("Boi - Assada","Leites - Com café e açúcar");
	}
	List<String[]> linhas(String arquivo) throws Exception {
		try(var input=getClass().getResourceAsStream("/db/changelog/dados/"+arquivo)){
			assertThat(input).isNotNull();return new String(input.readAllBytes(),StandardCharsets.UTF_8).lines().skip(1).map(l->l.split(";",-1)).toList();
		}
	}
}
