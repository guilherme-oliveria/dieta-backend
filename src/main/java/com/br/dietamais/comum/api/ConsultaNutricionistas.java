package com.br.dietamais.comum.api;

import java.util.*;

public interface ConsultaNutricionistas {
	Map<UUID,DadosNutri> porIds(Set<UUID> ids);
	List<UUID> daOrganizacaoAtual();
	/** Orientação padrão da nutri (RN-DIE-09); vazio se não configurada. */
	Optional<String> orientacaoPadrao(UUID nutri);
	record DadosNutri(UUID id,String nome,String crn,UUID organizacaoId) { }
}
