package com.br.dietamais.alimentos.aplicacao;

import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.alimentos.infra.ListasAlimentosStore;
import com.br.dietamais.comum.api.*;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ListaSubstituicaoEfetivaBanco implements ListaSubstituicaoEfetiva {
	private final ListasAlimentosStore listas;
	public ListaSubstituicaoEfetivaBanco(ListasAlimentosStore listas){this.listas=listas;}
	@Override public ListaSubstituicaoResposta consultar(UUID vinculoId,UUID pacienteId,UUID nutri){
		var ajuste=listas.ajuste(vinculoId,nutri,false);if(ajuste==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		return new ListaSubstituicaoResposta(pacienteId,ajuste.versao(),listas.efetivos(ajuste,nutri),ajuste.adicionados(),ajuste.removidos());
	}
}
