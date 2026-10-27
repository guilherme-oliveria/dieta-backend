package com.br.dietamais.alimentos.aplicacao;

import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.alimentos.dominio.*;
import com.br.dietamais.alimentos.infra.*;
import com.br.dietamais.comum.api.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

@Service
public class GerenciarAlimentos {
	private final ListasAlimentosStore listas;private final CatalogoAlimentosStore grupos;private final UsuarioAtual usuario;
	private final GuardaEscritaNutri guarda;private final GeradorIds ids;private final Clock relogio;private final JsonMapper mapper;
	public GerenciarAlimentos(ListasAlimentosStore listas,CatalogoAlimentosStore grupos,UsuarioAtual usuario,GuardaEscritaNutri guarda,GeradorIds ids,Clock relogio,JsonMapper mapper){
		this.listas=listas;this.grupos=grupos;this.usuario=usuario;this.guarda=guarda;this.ids=ids;this.relogio=relogio;this.mapper=mapper;
	}
	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public PaginaAlimentosResposta listar(int pagina,int tamanho,UUID grupo,String busca,boolean ativo){
		var nutri=usuario.obter().exigirNutricionista();
		if(pagina<0||tamanho<1||tamanho>100||busca!=null&&busca.length()>150)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		if(grupo!=null)grupoProprio(grupo,nutri,false);
		return listas.listar(nutri,pagina,tamanho,grupo,busca,ativo);
	}
	@Transactional
	public AlimentoResposta criar(CriarAlimentoRequisicao requisicao){
		var nutri=usuario.obter().exigirNutricionista();guarda.validar();listas.bloquear(nutri);
		var dados=new DadosAlimento(requisicao.grupoId(),requisicao.nome(),requisicao.medidaCaseira(),requisicao.gramasPorPorcao(),true);
		grupoProprio(dados.grupoId(),nutri,false);UUID id=requisicao.id()==null?ids.novo():requisicao.id();
		if(id.version()!=7)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		if(!listas.criar(id,nutri,dados,relogio.instant()))conflito(alimentoProprio(id,nutri,false));
		return alimentoProprio(id,nutri,false);
	}
	@Transactional
	public AlimentoResposta atualizar(UUID id,String ifMatch,AtualizarAlimentoRequisicao requisicao){
		var nutri=usuario.obter().exigirNutricionista();guarda.validar();listas.bloquear(nutri);
		var atual=alimentoProprio(id,nutri,true);if(VersaoLista.ler(ifMatch)!=atual.versao())conflito(atual);
		if(requisicao.ativo()==null)throw new ExcecaoNegocio(CodigoErro.OBRIGATORIO,null);
		var dados=new DadosAlimento(requisicao.grupoId(),requisicao.nome(),requisicao.medidaCaseira(),requisicao.gramasPorPorcao(),requisicao.ativo());
		grupoProprio(dados.grupoId(),nutri,false);listas.atualizar(id,nutri,dados,relogio.instant());return alimentoProprio(id,nutri,false);
	}
	@Transactional
	public GrupoAlimentoResposta atualizarMacros(UUID id,String ifMatch,AtualizarMacrosGrupoRequisicao requisicao){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();guarda.validar();listas.bloquear(nutri);
		var atual=grupoProprio(id,nutri,true);if(VersaoLista.ler(ifMatch)!=atual.versao())conflito(atual);
		for(var valor:Arrays.asList(requisicao.carboidratosPorPorcao(),requisicao.proteinasPorPorcao(),requisicao.gordurasPorPorcao()))
			if(valor==null||valor.signum()<0||valor.stripTrailingZeros().scale()>4||valor.compareTo(new BigDecimal("100000000"))>=0)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-ALI-01");
		grupos.atualizar(id,nutri,contexto.usuarioId(),requisicao,relogio.instant());return grupoProprio(id,nutri,false);
	}
	private GrupoAlimentoResposta grupoProprio(UUID id,UUID nutri,boolean bloquear){
		var grupo=grupos.grupo(id,nutri,bloquear);if(grupo==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);return grupo;
	}
	private AlimentoResposta alimentoProprio(UUID id,UUID nutri,boolean bloquear){
		var alimento=listas.alimento(id,nutri,bloquear);if(alimento==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);return alimento;
	}
	private void conflito(Object atual){throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02","A configuração foi atualizada. Revise os dados atuais.",List.of(),mapper.valueToTree(atual));}
}
