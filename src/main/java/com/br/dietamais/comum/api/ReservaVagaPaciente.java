package com.br.dietamais.comum.api;

/** Valida a vaga e mantém a organização bloqueada até o fim da transação chamadora. */
public interface ReservaVagaPaciente {
	void validar();
	void validarExistente();
}
