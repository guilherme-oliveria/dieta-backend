package com.br.dietamais.pacientes.dominio;

import com.br.dietamais.comum.api.*;

public final class VersaoFicha {
	private VersaoFicha(){}
	public static long ler(String cabecalho) {
		if(cabecalho!=null&&cabecalho.matches("\"[0-9]+\"")) {
			try {return Long.parseLong(cabecalho.substring(1,cabecalho.length()-1));}
			catch(NumberFormatException ignorada) { }
		}
		throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SYN-02","Informe If-Match com uma versão válida entre aspas.",java.util.List.of(),null);
	}
}
