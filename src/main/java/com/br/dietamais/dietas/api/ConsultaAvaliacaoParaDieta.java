package com.br.dietamais.dietas.api;

import java.util.Optional;
import java.util.UUID;

/**
 * A dieta pede, e o módulo de avaliações responde (BE-49d): peso dos "por kg", metas e condição de gestante ou lactante da avaliação mais
 * recente do paciente. Fica em {@code dietas.api} para a dependência seguir de avaliações para dietas e não fechar ciclo. Sempre pela nutri
 * informada; avaliação de outra nutri não aparece.
 */
public interface ConsultaAvaliacaoParaDieta {
	Optional<ReferenciaAvaliacaoDietaResposta> maisRecente(UUID nutricionista,UUID paciente);
}
