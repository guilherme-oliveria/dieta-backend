package com.br.dietamais.comum.api;

import java.util.*;

public interface ConsultaNutricionistas {
	Map<UUID,DadosNutri> porIds(Set<UUID> ids);
	List<UUID> daOrganizacaoAtual();
	/** Nutris de uma organização, para quem age sem usuário logado (webhook, jobs). */
	List<UUID> daOrganizacao(UUID organizacaoId);
	/** Orientação padrão da nutri (RN-DIE-09); vazio se não configurada. */
	Optional<String> orientacaoPadrao(UUID nutri);
	/** Chave da logo no {@link Armazenamento}; vazio se a nutri não enviou. */
	Optional<String> logoChave(UUID nutri);
	record DadosNutri(UUID id,String nome,String crn,UUID organizacaoId) { }
}
