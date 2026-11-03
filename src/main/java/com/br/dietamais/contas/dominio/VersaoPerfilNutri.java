package com.br.dietamais.contas.dominio;

import com.br.dietamais.comum.api.*;

public final class VersaoPerfilNutri {
	private VersaoPerfilNutri(){}
	public static long ler(String cabecalho){
		if(cabecalho!=null&&cabecalho.matches("\"[0-9]+\"")){
			try{return Long.parseLong(cabecalho.substring(1,cabecalho.length()-1));}
			catch(NumberFormatException ignorada){}
		}
		throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SYN-02");
	}
}
