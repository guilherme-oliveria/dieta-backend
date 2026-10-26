package com.br.dietamais.comum.api;

import java.util.List;

public interface DiarioSync {
	long marcoAtual();
	List<ReferenciaSync> mudancasDesde(ColecaoSync colecao,long seq,ContextoUsuario usuario,int limite);
}
