package com.br.dietamais.alimentos.api;

import java.util.UUID;

public interface ListaSubstituicaoEfetiva {
	/** Lista de substituição efetiva (alimentos ativos) do vínculo naquele instante, para congelar no snapshot (RN-DIE-03). */
	ListaSubstituicaoResposta consultar(UUID vinculoId,UUID pacienteId,UUID nutri);
}
