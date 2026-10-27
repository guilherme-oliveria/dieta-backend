package com.br.dietamais.contas.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.api.*;
import com.br.dietamais.contas.dominio.*;
import com.br.dietamais.contas.infra.PerfilNutriStore;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class GerenciarPerfilNutri {
	private final UsuarioAtual usuario;
	private final GuardaEscritaNutri guarda;
	private final PerfilNutriStore store;
	private final Clock relogio;
	private final JsonMapper mapper;
	public GerenciarPerfilNutri(UsuarioAtual usuario,GuardaEscritaNutri guarda,PerfilNutriStore store,Clock relogio,JsonMapper mapper){
		this.usuario=usuario;this.guarda=guarda;this.store=store;this.relogio=relogio;this.mapper=mapper;
	}
	@Transactional(readOnly=true)
	public PerfilNutriResposta consultar(){return proprio(usuario.obter(),false);}
	@Transactional
	public PerfilNutriResposta atualizar(String ifMatch,AtualizarPerfilNutriRequisicao requisicao){
		var contexto=usuario.obter();contexto.exigirNutricionista();guarda.validar();
		var atual=proprio(contexto,true);
		if(VersaoPerfilNutri.ler(ifMatch)!=atual.versao())throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02",
			"O perfil foi atualizado. Revise os dados atuais.",List.of(),mapper.valueToTree(atual));
		store.atualizar(contexto.exigirNutricionista(),contexto.usuarioId(),DadosPerfilNutri.de(requisicao),relogio.instant());
		return proprio(contexto,false);
	}
	private PerfilNutriResposta proprio(ContextoUsuario contexto,boolean bloquear){
		var perfil=store.consultar(contexto.exigirNutricionista(),contexto.usuarioId(),contexto.organizacaoId(),bloquear);
		if(perfil==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		return perfil;
	}
}
