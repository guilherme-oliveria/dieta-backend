package com.br.dietamais.referencias;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.TestcontainersConfiguration;
import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** O banco (changeset 030) carrega o mesmo CSV que os testes de cálculo usam: as consultas têm de dar o mesmo resultado. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReferenciasBancoTest {
	@Autowired ConsultaReferencias banco;
	@Autowired JdbcTemplate jdbc;
	@Test void bancoTemTodasAsLinhasDoCsvEResponde(){
		var csv=TabelaFaixas.doCsv(getClass().getClassLoader().getResourceAsStream(TabelaFaixas.CSV_ADULTO));
		assertThat(jdbc.queryForObject("select count(*) from referencia_faixa",Integer.class)).isEqualTo(118);
		assertThat(banco.versaoVigente()).isEqualTo("adulto-1.0.0").isEqualTo(csv.versaoVigente());
		for(var caso:new Object[][]{{"imc_adulto",null,30,"39.5"},{"imc_idoso",null,66,"29.01"},{"cintura","feminino",40,"88"},{"compleicao","masculino",30,"10.4"},
				{"percentual_gordura_adulto","feminino",50,"38"},{"percentual_gordura_adulto","masculino",40,"29.01"},{"risco_imc_adulto",null,30,"35"}}){
			var v=new BigDecimal((String)caso[3]);
			assertThat(banco.classificar((String)caso[0],(String)caso[1],(Integer)caso[2],v)).isEqualTo(csv.classificar((String)caso[0],(String)caso[1],(Integer)caso[2],v)).isPresent();
		}
	}
}
