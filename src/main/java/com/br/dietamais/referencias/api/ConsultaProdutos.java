package com.br.dietamais.referencias.api;

import java.util.*;

/** Consulta ao catálogo base de produtos e suplementos (BE-48a). Só leitura; as linhas vêm do banco (`referencia_produto`). */
public interface ConsultaProdutos {
	/**
	 * Busca por palavras do nome ou da marca (todas precisam aparecer, em qualquer ordem, sem diferenciar maiúsculas e acentos) e, opcionalmente, por
	 * classificação e apresentação exatas; ordem da planilha, no máximo {@code limite}.
	 */
	List<ProdutoReferencia> buscar(String trecho,String classificacao,String apresentacao,int limite);
	Optional<ProdutoReferencia> porNumero(int numero);
	List<String> classificacoes();
	List<String> apresentacoes();
}
