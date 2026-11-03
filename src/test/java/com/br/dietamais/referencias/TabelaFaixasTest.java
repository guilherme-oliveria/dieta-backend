package com.br.dietamais.referencias;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.referencias.api.TabelaFaixas;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class TabelaFaixasTest {
	static final TabelaFaixas T=TabelaFaixas.doCsv(TabelaFaixasTest.class.getClassLoader().getResourceAsStream(TabelaFaixas.CSV_ADULTO));
	String c(String tabela,String sexo,Integer idade,String v){return T.classificar(tabela,sexo,idade,new BigDecimal(v)).orElse(null);}

	@Test void imcAdultoNosLimites(){
		assertThat(c("imc_adulto",null,30,"15.99")).isEqualTo("MG_III");assertThat(c("imc_adulto",null,30,"16")).isEqualTo("MG_II");
		assertThat(c("imc_adulto",null,30,"18.5")).isEqualTo("NO");assertThat(c("imc_adulto",null,30,"39.99")).isEqualTo("OB_II");
		assertThat(c("imc_adulto",null,30,"40")).isEqualTo("OB_III");
	}
	@Test void imcIdosoPorFaixaDeIdadeComLimiteSuperiorFechado(){
		assertThat(c("imc_idoso",null,62,"22.99")).isEqualTo("MG");assertThat(c("imc_idoso",null,62,"28")).isEqualTo("NO");assertThat(c("imc_idoso",null,64,"28.01")).isEqualTo("SP");
		assertThat(c("imc_idoso",null,65,"23.99")).isEqualTo("MG");assertThat(c("imc_idoso",null,80,"29")).isEqualTo("NO");assertThat(c("imc_idoso",null,80,"29.01")).isEqualTo("SP");
		assertThat(c("imc_idoso",null,59,"25")).isNull();
	}
	@Test void riscoPorImc(){
		assertThat(c("risco_imc_adulto",null,30,"17")).isEqualTo("B");assertThat(c("risco_imc_adulto",null,30,"27.52")).isEqualTo("A");assertThat(c("risco_imc_adulto",null,30,"39.5")).isEqualTo("S");
		assertThat(c("risco_imc_adulto",null,30,"42")).isEqualTo("MS");assertThat(c("risco_imc_idoso",null,70,"25")).isEqualTo("ME");
	}
	@Test void cinturaECompleicaoDependemDoSexo(){
		assertThat(c("cintura","feminino",30,"80")).isEqualTo("A");assertThat(c("cintura","masculino",30,"80")).isEqualTo("B");assertThat(c("cintura","masculino",30,"102")).isEqualTo("MA");
		assertThat(c("compleicao","feminino",30,"11")).isEqualTo("MEDIA");assertThat(c("compleicao","feminino",30,"11.01")).isEqualTo("PEQUENA");assertThat(c("compleicao","masculino",30,"9.59")).isEqualTo("GRANDE");
	}
	@Test void percentualDeGorduraPorSexoEFaixaEtaria(){
		assertThat(c("percentual_gordura_adulto","masculino",29,"26.48")).isEqualTo("ALTO");assertThat(c("percentual_gordura_adulto","masculino",20,"24")).isEqualTo("ALTO");
		assertThat(c("percentual_gordura_adulto","masculino",20,"24.01")).isEqualTo("MT_ALTO");assertThat(c("percentual_gordura_adulto","feminino",17,"20")).isNull();
		assertThat(c("percentual_gordura_adulto","feminino",60,"23")).isEqualTo("MT_BOM");assertThat(c("percentual_gordura_adulto","feminino",30,"13")).isEqualTo("MT_BXO");
		assertThat(c("percentual_gordura_adulto",null,30,"20")).isNull();
	}
	@Test void tabelaDesconhecidaEVersao(){
		assertThat(c("nao_existe",null,30,"1")).isNull();assertThat(T.versaoVigente()).isEqualTo("adulto-1.0.0");
	}
}
