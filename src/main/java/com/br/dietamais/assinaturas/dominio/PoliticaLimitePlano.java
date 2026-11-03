package com.br.dietamais.assinaturas.dominio;

import com.br.dietamais.comum.api.*;

public final class PoliticaLimitePlano {
	private PoliticaLimitePlano(){}
	public static void validar(int ocupados,int limite) {
		if(ocupados>=limite)throw new ExcecaoNegocio(CodigoErro.LIMITE_PACIENTES_ATINGIDO,"RN-ASS-03");
	}
}
