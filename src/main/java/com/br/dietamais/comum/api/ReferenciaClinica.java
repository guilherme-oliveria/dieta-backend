package com.br.dietamais.comum.api;

import java.util.Objects;
import java.util.UUID;

public record ReferenciaClinica(String entidade,UUID registroId,UUID nutricionistaId,UUID pacienteId,UUID organizacaoId) {
	public ReferenciaClinica {
		if(entidade==null||!entidade.matches("[a-z_][a-z0-9_]{0,99}"))throw new IllegalArgumentException("Entidade inválida para auditoria");
		Objects.requireNonNull(registroId);
	}
}
