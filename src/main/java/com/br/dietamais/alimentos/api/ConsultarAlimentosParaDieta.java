package com.br.dietamais.alimentos.api;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface ConsultarAlimentosParaDieta {
	/** Só devolve alimentos da nutri informada; ids de outra nutri ou inexistentes ficam de fora. */
	Map<UUID,AlimentoParaDieta> consultar(UUID nutri,Collection<UUID> ids);
}
