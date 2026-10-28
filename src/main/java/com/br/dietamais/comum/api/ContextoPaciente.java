package com.br.dietamais.comum.api;

import java.util.UUID;

/** Monta o contexto de um paciente para trabalho em segundo plano, onde não há requisição (exportação de dados). */
public interface ContextoPaciente {
	ContextoUsuario de(UUID usuarioId);
}
