package com.br.dietamais.comum.api;

import java.util.*;

public interface ConsultaNutricionistas {
	Map<UUID,DadosNutri> porIds(Set<UUID> ids);
	List<UUID> daOrganizacaoAtual();
	record DadosNutri(UUID id,String nome,String crn,UUID organizacaoId) { }
}
