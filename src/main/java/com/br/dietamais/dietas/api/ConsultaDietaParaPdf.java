package com.br.dietamais.dietas.api;

import java.util.UUID;

/** Snapshot publicado de uma dieta, para o PDF (BE-28). Sempre filtrado pelo dono vindo de {@code UsuarioAtual}; alheio ou inexistente dá 404. */
public interface ConsultaDietaParaPdf {
	/** Nutri autenticada: o snapshot da dieta dela. 404 se a dieta nunca foi publicada. */
	DietaSnapshotResposta daNutri(UUID dietaId);
	/** Paciente autenticado: o snapshot que recebeu (vínculo ativo ou encerrado). */
	DietaSnapshotResposta doPaciente(UUID dietaId);
}
