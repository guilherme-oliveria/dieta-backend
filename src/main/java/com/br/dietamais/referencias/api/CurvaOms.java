package com.br.dietamais.referencias.api;

import java.math.BigDecimal;
import java.util.List;

/**
 * Linha da curva de crescimento da OMS (BE-54) para um indicador, sexo e idade. Os cortes vêm como estão nas abas P.I., E.I. e I.M.C. da planilha:
 * percentis 0,1, 3, 10, 15, 50, 85, 97 e 99 e escores Z de -3 a +3. Peso em kg, estatura em cm, IMC em kg/m².
 * {@code idadeMeses} é a idade da linha usada (de 10 a 17 anos a tabela é anual e vale a linha do ano completo).
 */
public record CurvaOms(String indicador,String sexo,int idadeMeses,List<BigDecimal> percentis,List<BigDecimal> escoresZ){
	public static final List<String> PERCENTIS=List.of("0,1","3","10","15","50","85","97","99");
	public static final List<String> ESCORES_Z=List.of("-3","-2","-1","0","+1","+2","+3");
	public static final List<String> INDICADORES=List.of("PESO_IDADE","ESTATURA_IDADE","IMC_IDADE");
}
