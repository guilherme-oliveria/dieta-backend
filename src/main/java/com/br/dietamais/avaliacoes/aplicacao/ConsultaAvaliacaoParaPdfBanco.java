package com.br.dietamais.avaliacoes.aplicacao;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.infra.AvaliacaoStore;
import com.br.dietamais.comum.api.*;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConsultaAvaliacaoParaPdfBanco implements ConsultaAvaliacaoParaPdf {
	private final GerenciarAvaliacoes avaliacoes;private final AvaliacaoStore store;private final UsuarioAtual usuario;
	public ConsultaAvaliacaoParaPdfBanco(GerenciarAvaliacoes avaliacoes,AvaliacaoStore store,UsuarioAtual usuario){this.avaliacoes=avaliacoes;this.store=store;this.usuario=usuario;}
	/** Reaproveita a consulta da nutri, que já audita a leitura. */
	@Override public AvaliacaoResposta daNutri(UUID id){return avaliacoes.consultar(id);}
	@Override @Transactional(readOnly=true)
	public AvaliacaoResposta doPaciente(UUID id){
		var a=store.compartilhadaDoPaciente(id,usuario.obter().exigirPaciente());
		if(a==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		return a;
	}
}
