package com.br.dietamais.comum.api;

public interface TermosUso {
	TermosUsoResposta consultar();
	void validar(Boolean aceitou,String versao);
}
