package com.br.dietamais.assinaturas.api;

import java.time.Instant;

public interface CriarOrganizacaoIndividual {
	AssinaturaResumoResposta criar(String nome, Instant agora);
}
