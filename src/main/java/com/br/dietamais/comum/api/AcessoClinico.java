package com.br.dietamais.comum.api;

import java.time.Instant;
import java.util.UUID;

public record AcessoClinico(UUID usuarioId,Perfil perfil,ReferenciaClinica recurso,String acao,
		Instant ocorridoEm,String origemIp,String traceId) {
	public AcessoClinico {
		if(!java.util.Set.of("LEITURA","ESCRITA").contains(acao))throw new IllegalArgumentException("Ação inválida para auditoria");
	}
}
