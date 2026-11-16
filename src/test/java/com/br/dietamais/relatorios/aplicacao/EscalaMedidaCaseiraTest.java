package com.br.dietamais.relatorios.aplicacao;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EscalaMedidaCaseiraTest {
	@ParameterizedTest
	@CsvSource({"¼ unidade,2,½ unidade", "½ unidade,2,1 unidade", "1 unidade,1.5,1½ unidade", "1½ unidade,2,3 unidade", "2 colheres,0.625,1¼ colheres", "¾ xícara,2,1½ xícara", "'1,5 colher',0.5,¾ colher", "fatia fina,1.5,1½ × fatia fina", "1 unidade,0.01,0 unidade"})
	void escalaNumeroInicialEArredondaParaQuarto(String medida,String fator,String esperado){
		assertThat(EscalaMedidaCaseira.escalar(medida,new BigDecimal(fator))).isEqualTo(esperado);
	}
}
