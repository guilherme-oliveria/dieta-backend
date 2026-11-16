package com.br.dietamais.alimentos;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.alimentos.dominio.*;
import com.br.dietamais.comum.api.ExcecaoNegocio;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Regras de forma do produto da nutri (RN-ALI-05), sem banco. */
class DadosProdutoTest {
	Map<String,BigDecimal> macros(){return new HashMap<>(Map.of("energia_kcal",new BigDecimal("100.50"),"carboidrato_g",BigDecimal.TEN,"proteina_g",new BigDecimal("2"),"lipideos_g",BigDecimal.ZERO));}
	DadosProduto novo(String nome,Map<String,BigDecimal> n){return new DadosProduto(nome,"  ",null,"pó",null,null,null,new BigDecimal("30"),null,n,true);}

	@Test void normalizaTextoENutrientes(){
		var d=novo("  Whey  ",macros());
		assertThat(d.nome()).isEqualTo("Whey");assertThat(d.marca()).isNull();assertThat(d.nutrientes().get("energia_kcal")).isEqualByComparingTo("100.5");
		assertThat(d.nutrientes()).containsKeys("lipideos_g");assertThatThrownBy(()->d.nutrientes().put("sodio_mg",BigDecimal.ONE)).isInstanceOf(UnsupportedOperationException.class);
	}
	@Test void recusaNomeVazioChaveDesconhecidaNegativoEFaltaDeMacro(){
		assertThatThrownBy(()->novo(" ",macros())).isInstanceOf(ExcecaoNegocio.class);
		var estranha=macros();estranha.put("inventado",BigDecimal.ONE);assertThatThrownBy(()->novo("x",estranha)).isInstanceOf(ExcecaoNegocio.class);
		var negativa=macros();negativa.put("sodio_mg",new BigDecimal("-0.1"));assertThatThrownBy(()->novo("x",negativa)).isInstanceOf(ExcecaoNegocio.class);
		var semMacro=macros();semMacro.remove("energia_kcal");assertThatThrownBy(()->novo("x",semMacro)).isInstanceOf(ExcecaoNegocio.class);
		var casas=macros();casas.put("sodio_mg",new BigDecimal("1.00001"));assertThatThrownBy(()->novo("x",casas)).isInstanceOf(ExcecaoNegocio.class);
	}
	@Test void asChavesCobremTodosOsNutrientesDoCatalogoBase() throws Exception {
		try(var in=getClass().getResourceAsStream("/db/changelog/dados/referencia-produto-nutriente.csv")){
			var chaves=new HashSet<String>();var l=new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\n");for(int i=1;i<l.length;i++)chaves.add(l[i].split(";")[1]);
			assertThat(NutrientesProduto.CHAVES).containsAll(chaves);assertThat(NutrientesProduto.CHAVES).hasSameSizeAs(chaves);
		}
	}
}
