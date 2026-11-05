package com.br.dietamais.pacientes.api;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Preenche {@code proximaConsulta} das fichas (BE-66); implementado pelo módulo de agenda. Vínculo sem consulta futura fica fora do mapa. */
public interface AgendaDosPacientes {
	Map<UUID,Instant> proximaConsulta(Collection<UUID> vinculos,UUID nutri,Instant agora);
}
