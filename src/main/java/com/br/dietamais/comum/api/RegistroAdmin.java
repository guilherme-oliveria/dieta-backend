package com.br.dietamais.comum.api;

import java.util.UUID;

/** Trilha de tudo que o admin altera (tabela {@code log_admin}, só inserção). O motivo é obrigatório. */
public interface RegistroAdmin {
	void registrar(String acao,String alvoTipo,UUID alvoId,Object antes,Object depois,String motivo);
}
