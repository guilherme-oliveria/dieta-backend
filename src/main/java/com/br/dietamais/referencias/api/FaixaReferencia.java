package com.br.dietamais.referencias.api;

import java.math.BigDecimal;

/** Uma linha de {@code referencia_faixa}. Limites nulos = sem limite. */
public record FaixaReferencia(int ordem,String tabela,String sexo,Integer idadeMin,Integer idadeMax,BigDecimal valorMin,
		boolean minInclusivo,BigDecimal valorMax,boolean maxInclusivo,String codigo,String fonte,String versaoReferencia) {
	public boolean contem(String sexoPaciente,Integer idade,BigDecimal valor){
		if(sexo!=null&&!sexo.equals(sexoPaciente))return false;
		if(idadeMin!=null||idadeMax!=null){
			if(idade==null)return false;
			if(idadeMin!=null&&idade<idadeMin)return false;
			if(idadeMax!=null&&idade>=idadeMax)return false;
		}
		if(valorMin!=null){int c=valor.compareTo(valorMin);if(c<0||(c==0&&!minInclusivo))return false;}
		if(valorMax!=null){int c=valor.compareTo(valorMax);if(c>0||(c==0&&!maxInclusivo))return false;}
		return true;
	}
}
