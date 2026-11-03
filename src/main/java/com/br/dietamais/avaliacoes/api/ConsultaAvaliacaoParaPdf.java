package com.br.dietamais.avaliacoes.api;

import java.util.UUID;

/** Avaliação para o PDF (BE-28), sempre pelo dono de {@code UsuarioAtual}; alheia ou inexistente dá 404. */
public interface ConsultaAvaliacaoParaPdf {
	AvaliacaoResposta daNutri(UUID id);
	/** RN-AVA-05: o paciente só obtém a avaliação que a nutri compartilhou. */
	AvaliacaoResposta doPaciente(UUID id);
}
