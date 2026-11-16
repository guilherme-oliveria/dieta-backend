package com.br.dietamais.alimentos.api;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface ConsultarProdutosParaDieta {
	/** Só devolve produtos da nutri informada; ids de outra nutri ou inexistentes ficam de fora. */
	Map<UUID,ProdutoParaDieta> consultar(UUID nutri,Collection<UUID> ids);
}
