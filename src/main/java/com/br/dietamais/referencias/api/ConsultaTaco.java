package com.br.dietamais.referencias.api;

import java.util.*;

/** Consulta à TACO (BE-47a). Só leitura; as linhas vêm do banco (`referencia_taco`). */
public interface ConsultaTaco {
	/** Busca por trecho do nome (sem diferenciar maiúsculas e acentos) e, opcionalmente, por categoria exata; ordem da tabela, no máximo {@code limite}. */
	List<AlimentoTaco> buscar(String trecho,String categoria,int limite);
	Optional<AlimentoTaco> porNumero(int numero);
	List<String> categorias();
}
