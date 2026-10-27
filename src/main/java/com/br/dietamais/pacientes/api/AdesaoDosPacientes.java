package com.br.dietamais.pacientes.api;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Preenche {@code adesao7d} das fichas; implementado pelo módulo de rotina. Vínculos sem dieta publicada ficam fora do mapa. */
public interface AdesaoDosPacientes {
	Map<UUID,BigDecimal> adesao7d(Collection<UUID> vinculos,ZoneId fuso);
}
