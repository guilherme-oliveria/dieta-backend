package com.br.dietamais.dietas.api;

import java.util.Optional;
import java.util.UUID;

/** Dieta em uso do paciente (S9) sem passar pelo usuário autenticado; usada por quem serve o paciente sem Bearer, como o feed .ics. */
public interface ConsultaDietaEmUso {
	Optional<DietaSnapshotResposta> snapshotEmUso(UUID paciente);
}
