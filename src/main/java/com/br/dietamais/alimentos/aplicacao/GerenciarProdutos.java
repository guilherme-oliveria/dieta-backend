package com.br.dietamais.alimentos.aplicacao;

import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.alimentos.dominio.*;
import com.br.dietamais.alimentos.infra.*;
import com.br.dietamais.comum.api.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

/** Produtos e suplementos próprios da nutri (BE-48b, RN-ALI-05): cria, edita e desativa; outra nutri nunca vê (404). */
@Service
public class GerenciarProdutos {
	private final ProdutosNutriStore store;private final ListasAlimentosStore listas;private final UsuarioAtual usuario;private final GuardaEscritaNutri guarda;
	private final GeradorIds ids;private final Clock relogio;private final JsonMapper mapper;
	public GerenciarProdutos(ProdutosNutriStore store,ListasAlimentosStore listas,UsuarioAtual usuario,GuardaEscritaNutri guarda,GeradorIds ids,Clock relogio,JsonMapper mapper){
		this.store=store;this.listas=listas;this.usuario=usuario;this.guarda=guarda;this.ids=ids;this.relogio=relogio;this.mapper=mapper;
	}
	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public PaginaProdutosResposta listar(int pagina,int tamanho,String busca,String classificacao,boolean ativo){
		var nutri=usuario.obter().exigirNutricionista();
		if(pagina<0||tamanho<1||tamanho>100||busca!=null&&busca.length()>150||classificacao!=null&&classificacao.length()>80)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		return store.listar(nutri,pagina,tamanho,busca,classificacao,ativo);
	}
	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public ProdutoResposta consultar(UUID id){return produtoProprio(id,usuario.obter().exigirNutricionista(),false);}
	@Transactional
	public ProdutoResposta criar(CriarProdutoRequisicao r){
		var nutri=usuario.obter().exigirNutricionista();guarda.validar();listas.bloquear(nutri);
		var dados=new DadosProduto(r.nome(),r.marca(),r.classificacao(),r.apresentacao(),r.embalagem(),r.sabores(),r.medidaCaseira(),r.medidaGramas(),r.observacao(),r.nutrientes(),true);
		UUID id=r.id()==null?ids.novo():r.id();
		if(id.version()!=7)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		if(!store.criar(id,nutri,dados,relogio.instant()))conflito(produtoProprio(id,nutri,false));
		return produtoProprio(id,nutri,false);
	}
	@Transactional
	public ProdutoResposta atualizar(UUID id,String ifMatch,AtualizarProdutoRequisicao r){
		var nutri=usuario.obter().exigirNutricionista();guarda.validar();listas.bloquear(nutri);
		var atual=produtoProprio(id,nutri,true);if(VersaoLista.ler(ifMatch)!=atual.versao())conflito(atual);
		if(r.ativo()==null)throw new ExcecaoNegocio(CodigoErro.OBRIGATORIO,null);
		var dados=new DadosProduto(r.nome(),r.marca(),r.classificacao(),r.apresentacao(),r.embalagem(),r.sabores(),r.medidaCaseira(),r.medidaGramas(),r.observacao(),r.nutrientes(),r.ativo());
		store.atualizar(id,nutri,dados,relogio.instant());return produtoProprio(id,nutri,false);
	}
	private ProdutoResposta produtoProprio(UUID id,UUID nutri,boolean bloquear){
		var p=store.produto(id,nutri,bloquear);if(p==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);return p;
	}
	private void conflito(Object atual){throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02","O produto foi atualizado. Revise os dados atuais.",List.of(),mapper.valueToTree(atual));}
}
