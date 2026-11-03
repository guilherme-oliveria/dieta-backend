package com.br.dietamais.dietas.aplicacao;

import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.dietas.dominio.*;
import com.br.dietamais.dietas.infra.ModeloDietaStore;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Modelos de dieta (BE-52, RN-MOD-01 a RN-MOD-03). Os do sistema são somente leitura e qualquer nutri os copia; a nutri edita só as cópias dela
 * (de outra nutri é 404). Aplicar a um paciente cria um rascunho pelo mesmo caminho da dieta, nunca publica.
 */
@Service
public class GerenciarModelosDieta {
	static final int MAX_MODELOS=200;
	private final ModeloDietaStore store;private final GerenciarDietas dietas;private final ConsultarAlimentosParaDieta alimentos;private final UsuarioAtual usuario;private final GuardaEscritaNutri guarda;
	private final GeradorIds ids;private final Clock relogio;private final JsonMapper mapper;
	public GerenciarModelosDieta(ModeloDietaStore store,GerenciarDietas dietas,ConsultarAlimentosParaDieta alimentos,UsuarioAtual usuario,GuardaEscritaNutri guarda,GeradorIds ids,Clock relogio,JsonMapper mapper){
		this.store=store;this.dietas=dietas;this.alimentos=alimentos;this.usuario=usuario;this.guarda=guarda;this.ids=ids;this.relogio=relogio;this.mapper=mapper;
	}
	@Transactional(readOnly=true)
	public ModelosDietaResposta listar(){
		var nutri=usuario.obter().exigirNutricionista();
		return new ModelosDietaResposta(store.visiveis(nutri).stream().map(this::resposta).toList());
	}
	@Transactional(readOnly=true)
	public ModeloDietaResposta consultar(UUID id){
		var nutri=usuario.obter().exigirNutricionista();return resposta(visivel(id,nutri));
	}
	@Transactional
	public ModeloDietaResposta criar(SalvarModeloDietaRequisicao r){
		var nutri=usuario.obter().exigirNutricionista();guarda.validar();store.bloquear(nutri);
		return inserir(nutri,r);
	}
	@Transactional
	public ModeloDietaResposta atualizar(UUID id,String ifMatch,SalvarModeloDietaRequisicao r){
		var nutri=usuario.obter().exigirNutricionista();guarda.validar();store.bloquear(nutri);
		var atual=propria(id,nutri);
		if(r.id()!=null&&!r.id().equals(id))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		long versao=VersaoDieta.ler(ifMatch);
		if(versao!=atual.versao())throw conflito(atual);
		var conteudo=conteudo(r,nutri);
		if(!store.atualizar(id,nutri,versao,conteudo,relogio.instant()))throw conflito(atual);
		return resposta(store.daNutri(id,nutri,false));
	}
	@Transactional
	public void excluir(UUID id,String ifMatch){
		var nutri=usuario.obter().exigirNutricionista();guarda.validar();store.bloquear(nutri);
		var atual=propria(id,nutri);long versao=VersaoDieta.ler(ifMatch);
		if(versao!=atual.versao()||!store.excluir(id,nutri,versao,relogio.instant()))throw conflito(atual);
	}
	/** Copia um modelo (do sistema ou dela) para a lista da nutri. */
	@Transactional
	public ModeloDietaResposta duplicar(UUID id){
		var nutri=usuario.obter().exigirNutricionista();guarda.validar();store.bloquear(nutri);
		var origem=visivel(id,nutri);var r=resposta(origem);
		String titulo=r.titulo();titulo=(titulo.length()>141?titulo.substring(0,141):titulo)+" (cópia)";
		return inserir(nutri,new SalvarModeloDietaRequisicao(null,titulo,r.descricao(),r.condicao(),r.orientacao(),r.refeicoes(),r.porcoes(),r.metas(),r.rodape(),r.restricoes(),r.kcalReferencia()));
	}
	/** Salva uma dieta dela como modelo; o total de energia da dieta vira a referência de escala. */
	@Transactional
	public ModeloDietaResposta salvarComoModelo(UUID dietaId,SalvarComoModeloRequisicao r){
		var d=dietas.consultar(dietaId);
		var refeicoes=d.refeicoes().stream().map(x->new RefeicaoRequisicao(x.id(),x.nome(),x.horario(),x.ordem(),x.itens().stream().map(i->new ItemRefeicaoRequisicao(null,i.alimentoId(),i.grupoId(),i.porcoes(),
			i.ehAVontade()?Boolean.TRUE:null,i.alimentoId()==null&&i.grupoId()==null?i.textoLivre():null,i.observacao())).toList(),x.alternativa()?"ALTERNATIVA":null,x.alternativaDe())).toList();
		var kcal=d.totais().kcal();
		var nutri=usuario.obter().exigirNutricionista();guarda.validar();store.bloquear(nutri);
		return inserir(nutri,new SalvarModeloDietaRequisicao(r.id(),r.titulo(),r.descricao(),r.condicao(),d.orientacao(),refeicoes,d.porcoes(),d.metas(),d.rodape(),r.restricoes(),kcal.signum()>0?kcal:null));
	}
	/**
	 * RN-MOD-02: cria um rascunho com as porções escaladas pelo alvo de energia (arredondando para 0,5). Alimentos que a nutri não tem mais ou que
	 * carregam uma restrição do modelo saem, com aviso (RN-MOD-03); itens por grupo ficam.
	 */
	@Transactional
	public DietaDeModeloResposta aplicar(UUID paciente,UUID modeloId,AplicarModeloRequisicao r){
		var nutri=usuario.obter().exigirNutricionista();
		var modelo=resposta(visivel(modeloId,nutri));var avisos=new ArrayList<AvisoModelo>();
		BigDecimal base=modelo.kcalReferencia()!=null?modelo.kcalReferencia():modelo.metas()==null?null:modelo.metas().vetKcal();
		BigDecimal fator=BigDecimal.ONE;
		if(r!=null&&r.vetKcal()!=null){
			if(base==null)avisos.add(new AvisoModelo("SEM_ESCALA","O modelo não tem energia de referência; as porções ficaram como no modelo."));
			else{
				fator=EscalaPorcoes.fator(r.vetKcal(),base);
				if(fator==null)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-MOD-02");
			}
		}
		var usados=new HashSet<UUID>();
		modelo.refeicoes().forEach(x->x.itens().forEach(i->{if(i.alimentoId()!=null)usados.add(i.alimentoId());}));
		var base_alimentos=alimentos.consultar(nutri,usados);
		var restricoes=EnumSet.noneOf(MarcadorAlimento.class);restricoes.addAll(modelo.restricoes());
		var novos=new HashMap<UUID,UUID>();for(var x:modelo.refeicoes())if(x.id()!=null)novos.put(x.id(),ids.novo());
		var refeicoes=new ArrayList<RefeicaoRequisicao>();
		for(var x:modelo.refeicoes()){
			var itens=new ArrayList<ItemRefeicaoRequisicao>();
			for(var i:x.itens()){
				if(i.alimentoId()!=null){
					var a=base_alimentos.get(i.alimentoId());
					if(a==null||!a.ativo()){avisos.add(new AvisoModelo("ITEM_INDISPONIVEL","Um alimento do modelo não está na sua lista e foi removido de "+x.nome()+"."));continue;}
					if(a.marcadores().stream().anyMatch(restricoes::contains)){avisos.add(new AvisoModelo("ITEM_REMOVIDO_RESTRICAO",a.nome()+" foi removido de "+x.nome()+" por causa da restrição do modelo."));continue;}
				}
				var porcoes=Boolean.TRUE.equals(i.aVontade())?i.porcoes():EscalaPorcoes.escalar(i.porcoes(),fator);
				itens.add(new ItemRefeicaoRequisicao(null,i.alimentoId(),i.grupoId(),porcoes,i.aVontade(),i.textoLivre(),i.observacao()));
			}
			refeicoes.add(new RefeicaoRequisicao(x.id()==null?null:novos.get(x.id()),x.nome(),x.horario(),x.ordem(),itens,x.tipo(),x.alternativaDe()==null?null:novos.get(x.alternativaDe())));
		}
		var metas=modelo.metas();
		if(metas!=null&&r!=null&&r.vetKcal()!=null)metas=new MetasDieta(r.vetKcal(),metas.carboidratoPct(),metas.proteinaPct(),metas.lipidioPct());
		var dieta=dietas.criar(paciente,new SalvarDietaRequisicao(r==null?null:r.id(),modelo.titulo(),modelo.orientacao(),refeicoes,modelo.porcoes(),metas,modelo.rodape(),modelo.restricoes().isEmpty()?null:modelo.restricoes()));
		return new DietaDeModeloResposta(dieta,fator,avisos);
	}
	private ModeloDietaResposta inserir(UUID nutri,SalvarModeloDietaRequisicao r){
		if(store.contar(nutri)>=MAX_MODELOS)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-MOD-01");
		UUID id=r.id()==null?ids.novo():r.id();if(id.version()!=7)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		var conteudo=conteudo(r,nutri);
		if(!store.inserir(id,nutri,conteudo,relogio.instant()))throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02");
		return resposta(store.daNutri(id,nutri,false));
	}
	/** Mesmas regras de conteúdo da dieta (RN-DIE-10, RN-DIE-11, RN-ALI-01/03), validadas pelo caminho da própria dieta. */
	private ModeloDietaStore.Conteudo conteudo(SalvarModeloDietaRequisicao r,UUID nutri){
		dietas.validar(new SalvarDietaRequisicao(null,r.titulo(),r.orientacao(),r.refeicoes(),r.porcoes(),r.metas(),r.rodape()),nutri);
		var restricoes=r.restricoes()==null?List.<MarcadorAlimento>of():r.restricoes().stream().distinct().sorted().toList();
		return new ModeloDietaStore.Conteudo(r.titulo().strip(),texto(r.descricao()),texto(r.condicao()),texto(r.orientacao()),texto(r.rodape()),
			r.porcoes()==null||r.porcoes().isEmpty()?null:mapper.writeValueAsString(r.porcoes()),r.metas()==null?null:mapper.writeValueAsString(r.metas()),
			restricoes.stream().map(Enum::name).toList(),r.kcalReferencia(),mapper.writeValueAsString(r.refeicoes()));
	}
	private ModeloDietaStore.Linha visivel(UUID id,UUID nutri){
		var l=store.visivel(id,nutri);if(l==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);return l;
	}
	/** RN-MOD-01: o modelo do sistema não se altera (403); o de outra nutri não existe para ela (404). */
	private ModeloDietaStore.Linha propria(UUID id,UUID nutri){
		var l=store.daNutri(id,nutri,true);
		if(l!=null)return l;
		if(store.visivel(id,nutri)!=null)throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-MOD-01");
		throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
	}
	private ExcecaoNegocio conflito(ModeloDietaStore.Linha atual){
		return new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02","O modelo foi atualizado. Revise os dados atuais.",List.of(),mapper.valueToTree(resposta(atual)));
	}
	/** Usada também pelo sync. */
	ModeloDietaResposta resposta(ModeloDietaStore.Linha l){
		return new ModeloDietaResposta(l.id(),l.nutricionistaId()==null?"SISTEMA":"NUTRI",l.titulo(),l.descricao(),l.condicao(),l.orientacao(),
			mapper.readValue(l.refeicoesJson(),new TypeReference<List<RefeicaoRequisicao>>(){}),
			l.porcoesJson()==null?null:mapper.readValue(l.porcoesJson(),new TypeReference<List<PorcaoGrupo>>(){}),
			l.metasJson()==null?null:mapper.readValue(l.metasJson(),MetasDieta.class),l.rodape(),
			l.restricoes().stream().map(MarcadorAlimento::valueOf).toList(),l.kcalReferencia(),l.versao(),l.criadoEm(),l.atualizadoEm());
	}
	private String texto(String valor){return valor==null||valor.isBlank()?null:valor.strip();}
}
