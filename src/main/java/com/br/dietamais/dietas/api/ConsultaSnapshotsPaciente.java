package com.br.dietamais.dietas.api;

import java.util.Optional;
import java.util.UUID;

public interface ConsultaSnapshotsPaciente {
	/** Snapshot publicado da dieta que pertence ao paciente (vínculo ativo ou encerrado); vazio se inexistente ou alheio. */
	Optional<SnapshotParaRotina> obter(UUID dietaId,UUID paciente);
	record SnapshotParaRotina(DietaSnapshotResposta snapshot,boolean vinculoAtivo,boolean dietaPublicada) {}
}
