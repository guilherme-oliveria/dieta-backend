package com.br.dietamais.sync.dominio;

import com.br.dietamais.comum.api.*;
import java.util.List;

public final class CursorSync {
	private CursorSync(){}
	public static long ler(String valor) {
		if(valor==null||valor.isEmpty())return 0;
		if(valor.matches("[0-9]{1,19}"))try{return Long.parseLong(valor);}catch(NumberFormatException ignorada){}
		throw invalido("cursor");
	}
	public static void validar(long cursor,long marco,int limite) {
		if(limite<1||limite>1000)throw invalido("limite");
		if(cursor>marco)throw invalido("cursor");
	}
	private static ExcecaoNegocio invalido(String campo){return new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SYN-01","Revise os parâmetros de sincronização.",List.of(new ErroCampoResposta(campo,"FORMATO_INVALIDO","Informe um valor válido.")),null);}
}
