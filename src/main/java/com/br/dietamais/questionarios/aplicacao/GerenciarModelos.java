package com.br.dietamais.questionarios.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.questionarios.api.*;
import com.br.dietamais.questionarios.dominio.*;
import com.br.dietamais.questionarios.infra.QuestionarioStore;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

/** Modelos de questionário da nutri (RN-ANA-01/02). A cópia do modelo padrão nasce no primeiro acesso, uma única vez por nutri. */
@Service
public class GerenciarModelos {
	private final QuestionarioStore store;private final UsuarioAtual usuario;private final GuardaEscritaNutri guarda;private final GeradorIds ids;private final Clock relogio;private final JsonMapper mapper;
	public GerenciarModelos(QuestionarioStore store,UsuarioAtual usuario,GuardaEscritaNutri guarda,GeradorIds ids,Clock relogio,JsonMapper mapper){
		this.store=store;this.usuario=usuario;this.guarda=guarda;this.ids=ids;this.relogio=relogio;this.mapper=mapper;
	}

	@Transactional
	public ModelosResposta listar(){
		var nutri=usuario.obter().exigirNutricionista();garantirPadrao(nutri);
		return new ModelosResposta(store.modelosDaNutri(nutri));
	}
	@Transactional
	public ModeloQuestionarioResposta consultar(UUID id){
		var nutri=usuario.obter().exigirNutricionista();garantirPadrao(nutri);return propria(id,nutri);
	}

	@Transactional
	public ModeloQuestionarioResposta criar(SalvarModeloRequisicao r){
		var nutri=usuario.obter().exigirNutricionista();guarda.validar();store.bloquear(nutri);garantirPadrao(nutri);
		UUID id=r.id()==null?ids.novo():idV7(r.id());
		var blocos=ValidadorModelo.normalizar(r.blocos(),ids::novo);
		if(!store.inserirModelo(id,nutri,r.nome().strip(),false,blocos,relogio.instant()))throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02");
		return propria(id,nutri);
	}

	@Transactional
	public ModeloQuestionarioResposta atualizar(UUID id,String ifMatch,SalvarModeloRequisicao r){
		var nutri=usuario.obter().exigirNutricionista();guarda.validar();store.bloquear(nutri);
		var versao=store.versaoDoModelo(id,nutri,true);if(versao==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		if(r.id()!=null&&!r.id().equals(id))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		if(versao(ifMatch)!=versao)throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02","O modelo foi atualizado. Revise os dados atuais.",List.of(),mapper.valueToTree(propria(id,nutri)));
		var blocos=ValidadorModelo.normalizar(r.blocos(),ids::novo);
		store.substituirModelo(id,nutri,r.nome().strip(),blocos,relogio.instant());
		return propria(id,nutri);
	}

	private void garantirPadrao(UUID nutri){
		if(store.temPadrao(nutri))return;
		store.bloquear(nutri);
		if(store.temPadrao(nutri))return;
		store.inserirModelo(ids.novo(),nutri,ModeloPadrao.NOME,true,ModeloPadrao.blocos(ids::novo),relogio.instant());
	}
	private ModeloQuestionarioResposta propria(UUID id,UUID nutri){
		var m=store.modelo(id,nutri);if(m==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);return m;
	}
	private UUID idV7(UUID id){if(id.version()!=7)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);return id;}
	static long versao(String valor){
		if(valor!=null&&valor.matches("\"[0-9]+\""))try{return Long.parseLong(valor.substring(1,valor.length()-1));}catch(NumberFormatException ignorada){}
		throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SYN-02");
	}
}
