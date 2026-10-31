package com.br.dietamais.avaliacoes.dominio;

import com.br.dietamais.avaliacoes.api.*;

/** Frases das macros {@code DIAG_*}. Só gera texto quando há cintura, como a célula B96 da planilha. */
public final class TextoResumoAvaliacao {
	private TextoResumoAvaliacao(){}
	public static String gerar(ClasseImc classe,RiscoComorbidades risco,ClasseCintura cintura,ClassePercentualGordura pg){
		if(cintura==null)return null;
		var imc=switch(classe){
			case MG->"magreza e ";case NO->"normalidade e ";case SP->"sobrepeso e ";
			case MG_III->"magreza grau III (grave) e ";case MG_II->"magreza grau II (moderada) e ";case MG_I->"magreza grau I (leve) e ";
			case OB_III->"obesidade grau III (grave) e ";case OB_II->"obesidade grau II (moderada) e ";case OB_I->"obesidade grau I (leve) e ";};
		var r=switch(risco){
			case B->"risco baixo";case ME->"risco médio";case A->"risco aumentado";case MO->"risco moderado";case S->"risco severo";
			case MS->"risco muito severo";}; // correção 3 (seção 3.6): a macro comparava com "MS I"
		var c=switch(cintura){
			case B->"e baixo risco de complicações ligadas ao excesso de gordura abdominal, segundo a circunferência da cintura.";
			case A->"e risco aumentado de complicações ligadas ao excesso de gordura abdominal, segundo a circunferência da cintura.";
			case MA->"e risco muito aumentado de complicações ligadas ao excesso de gordura abdominal, segundo a circunferência da cintura.";};
		var p=pg==null?"":" Seu percentual de gordura é considerado "+switch(pg){
			case MT_BXO->"muito baixo";case EXC->"excelente";case MT_BOM->"muito bom";case BOM->"bom";case ADEQ->"adequado";
			case MOD_ALTO->"moderadamente alto";case ALTO->"alto";case MT_ALTO->"muito alto";}+" para sua idade.";
		return "O avaliado apresenta "+imc+r+" de desenvolver comorbidades relacionadas ao excesso de peso, segundo o IMC "+c+p;
	}
}
