package com.br.dietamais.comum.api;

import java.util.List;

public interface FonteSync<T> {
	ColecaoSync colecao();
	List<ReferenciaSync> mudancasDesde(long seq,ContextoUsuario usuario,int limite);
	T carregar(ReferenciaSync referencia,ContextoUsuario usuario);
}
