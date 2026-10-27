package com.br.dietamais.contas.dominio;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.br.dietamais.comum.api.ExcecaoNegocio;

class PerfilNutriTest {
	@Test void versaoForteAceitaZeroERejeitaFormatosAmbiguosOuOverflow(){
		assertThat(VersaoPerfilNutri.ler("\"0\"")).isZero();assertThat(VersaoPerfilNutri.ler("\"42\"")).isEqualTo(42);
		for(String valor:new String[]{null,"","0","*","W/\"1\"","\"1\",\"2\"","\"-1\"","\"9223372036854775808\""})assertThatThrownBy(()->VersaoPerfilNutri.ler(valor)).isInstanceOf(ExcecaoNegocio.class);
	}
	@Test void camposOpcionaisVaziosSaoNulosSemTruncarConteudo(){
		assertThat(new DadosPerfilNutri(" Nome fictício "," CRN-1 00000 ","   "," Texto fictício ")).isEqualTo(new DadosPerfilNutri("Nome fictício","CRN-1 00000",null,"Texto fictício"));
		assertThatThrownBy(()->new DadosPerfilNutri(" ","CRN-1 00000",null,null)).isInstanceOf(ExcecaoNegocio.class);
		assertThatThrownBy(()->new DadosPerfilNutri("Fictício","CRN-invalido",null,null)).isInstanceOf(ExcecaoNegocio.class);
		assertThatThrownBy(()->new DadosPerfilNutri("Fictício","CRN-1 00000","1".repeat(31),null)).isInstanceOf(ExcecaoNegocio.class);
	}
}
