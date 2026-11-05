package com.br.dietamais.pacientes.api;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Preenche {@code ultimaAvaliacao} das fichas (BE-66); implementado pelo módulo de avaliações. Vínculo sem avaliação fica fora do mapa. */
public interface UltimaAvaliacaoDosPacientes {
	Map<UUID,UltimaAvaliacaoResumo> ultima(Collection<UUID> vinculos,UUID nutri);
}
