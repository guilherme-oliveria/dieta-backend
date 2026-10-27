package com.br.dietamais.alimentos.dominio;

import com.br.dietamais.comum.api.*;

public final class VersaoLista {
	private VersaoLista(){}
	public static long ler(String valor){
		if(valor!=null&&valor.matches("\"[0-9]+\""))try{return Long.parseLong(valor.substring(1,valor.length()-1));}catch(NumberFormatException ignorada){}
		throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SYN-02");
	}
}
