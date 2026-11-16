package com.br.dietamais.referencias.api;

import java.util.*;

/** Consulta à tabela de MET (BE-44). Só leitura; as linhas vêm do banco (`referencia_met`). */
public interface ConsultaAtividadesMet {
	/** Todas as atividades, na ordem alfabética da planilha. */
	List<AtividadeMet> listar();
	Optional<AtividadeMet> buscar(String codigo);
}
