package com.br.dietamais.referencias.api;

import java.math.BigDecimal;

/** Percentis de Frisancho (1981) de um indicador, sexo e idade (BE-54b). {@code p5}, {@code p10} e {@code p90} só existem para AMBC e AGB; CB, DCT e DCSE têm só a mediana. */
public record PercentisFrisancho(String indicador,String sexo,int idadeAnos,BigDecimal p5,BigDecimal p10,BigDecimal p50,BigDecimal p90){
	public static final java.util.List<String> INDICADORES=java.util.List.of("CB","DCT","DCSE","AMBC","AGB");
}
