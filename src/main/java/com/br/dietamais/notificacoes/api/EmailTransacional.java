package com.br.dietamais.notificacoes.api;

import java.time.Instant;

public record EmailTransacional(TipoEmail tipo, String destinatario, String token, Instant expiraEm) {
	@Override public String toString() { return "EmailTransacional[dados protegidos]"; }
}
