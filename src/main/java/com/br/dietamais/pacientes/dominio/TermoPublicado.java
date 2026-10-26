package com.br.dietamais.pacientes.dominio;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.TermoConsentimentoResposta;

public final class TermoPublicado {
	private TermoPublicado(){}
	public static TermoConsentimentoResposta exigir(String versao,String texto) {
		if(versao==null||versao.isBlank()||versao.length()>40||texto==null||texto.isBlank())
			throw new ExcecaoNegocio(CodigoErro.CONSENTIMENTO_OBRIGATORIO,"RN-VIN-04","O consentimento ainda não está disponível.",java.util.List.of(),null);
		return new TermoConsentimentoResposta(versao,texto);
	}
}
