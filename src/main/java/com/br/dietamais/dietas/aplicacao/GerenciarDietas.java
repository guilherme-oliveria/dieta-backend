package com.br.dietamais.dietas.aplicacao;

import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.dietas.dominio.*;
import com.br.dietamais.dietas.dominio.LinhasDieta.*;
import com.br.dietamais.dietas.infra.DietaStore;
import com.br.dietamais.pacientes.api.*;
import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

@Service
public class GerenciarDietas {
	static final int MAX_REFEICOES=20,MAX_ITENS=60;
	private final ConsultaPorcoes porcoes;private final MontadorDieta montador;private final DietaStore dietas;private final ConsultarAlimentosParaDieta alimentos;private final ConsultarFichaParaLista fichas;private final UsuarioAtual usuario;
	private final GuardaEscritaNutri guarda;private final AuditoriaClinica auditoria;private final GeradorIds ids;private final Clock relogio;private final JsonMapper mapper;
	public GerenciarDietas(ConsultaPorcoes porcoes,MontadorDieta montador,DietaStore dietas,ConsultarAlimentosParaDieta alimentos,ConsultarFichaParaLista fichas,UsuarioAtual usuario,GuardaEscritaNutri guarda,AuditoriaClinica auditoria,GeradorIds ids,Clock relogio,JsonMapper mapper){
		this.porcoes=porcoes;this.montador=montador;this.dietas=dietas;this.alimentos=alimentos;this.fichas=fichas;this.usuario=usuario;this.guarda=guarda;this.auditoria=auditoria;this.ids=ids;this.relogio=relogio;this.mapper=mapper;
	}
	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public DietasResposta listar(UUID paciente){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();var ficha=fichas.consultar(paciente,false);
		var lista=dietas.porVinculo(ficha.vinculoId(),nutri);var respostas=montador.montar(lista,nutri);
		auditoria.leitura(new ReferenciaClinica("dieta",ficha.vinculoId(),nutri,ficha.pacienteId(),contexto.organizacaoId()));
		return new DietasResposta(respostas);
	}
	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public DietaResposta consultar(UUID id){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();var dieta=propria(id,nutri,false);
		var resposta=montador.montar(List.of(dieta),nutri).get(0);auditoria.leitura(referencia(dieta,contexto));return resposta;
	}
	@Transactional
	public DietaResposta criar(UUID paciente,SalvarDietaRequisicao requisicao){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();guarda.validar();dietas.bloquear(nutri);
		var ficha=fichas.consultar(paciente,true);
		if(ficha.status()==StatusVinculo.ENCERRADO)throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-VIN-05");
		UUID id=requisicao.id()==null?ids.novo():idV7(requisicao.id());var agora=relogio.instant();
		var conteudo=validar(requisicao,nutri);
		if(!dietas.inserir(id,ficha.vinculoId(),nutri,ficha.pacienteId(),requisicao.titulo().strip(),texto(requisicao.orientacao()),conteudo.extras(),agora))throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02");
		gravar(id,requisicao,conteudo);
		var dieta=propria(id,nutri,false);var resposta=montador.montar(List.of(dieta),nutri).get(0);auditoria.escrita(referencia(dieta,contexto));return resposta;
	}
	@Transactional
	public DietaResposta atualizar(UUID id,String ifMatch,SalvarDietaRequisicao requisicao){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();guarda.validar();dietas.bloquear(nutri);
		var atual=propria(id,nutri,true);
		if(requisicao.id()!=null&&!requisicao.id().equals(id))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		if(atual.status()!=StatusDieta.RASCUNHO)throw new ExcecaoNegocio(CodigoErro.DIETA_NAO_EDITAVEL,"RN-DIE-04");
		var ficha=fichas.consultar(atual.pacienteId(),true);
		// RN-VIN-05: um vínculo novo não reabre a edição do histórico do vínculo anterior.
		if(!ficha.vinculoId().equals(atual.vinculoId())||ficha.status()==StatusVinculo.ENCERRADO)throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-VIN-05");
		if(VersaoDieta.ler(ifMatch)!=atual.versao()){
			auditoria.leitura(referencia(atual,contexto));
			throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02","A dieta foi atualizada. Revise os dados atuais.",List.of(),mapper.valueToTree(montador.montar(List.of(atual),nutri).get(0)));
		}
		var conteudo=validar(requisicao,nutri);
		dietas.limparConteudo(id);gravar(id,requisicao,conteudo);dietas.atualizar(id,nutri,requisicao.titulo().strip(),texto(requisicao.orientacao()),conteudo.extras(),relogio.instant());
		var dieta=propria(id,nutri,false);var resposta=montador.montar(List.of(dieta),nutri).get(0);auditoria.escrita(referencia(dieta,contexto));return resposta;
	}
	private record Conteudo(DietaStore.Extras extras) {}
	/**
	 * RN-ALI-01/03: itens por alimento só de alimentos ativos da própria lista; a existência em outra nutri é indistinguível de inexistente.
	 * RN-DIE-10: cada item traz exatamente uma origem e item por grupo exige a porção escolhida. RN-DIE-11: alternativa aponta para uma refeição principal desta dieta.
	 */
	private Conteudo validar(SalvarDietaRequisicao r,UUID nutri){
		if(r.refeicoes().size()>MAX_REFEICOES)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-DIE-01");
		var catalogo=new HashMap<UUID,Map<Integer,PorcoesGrupos.Opcao>>();
		porcoes.porcoes().grupos().forEach(g->{var o=new HashMap<Integer,PorcoesGrupos.Opcao>();g.opcoes().forEach(x->o.put(x.opcao(),x));catalogo.put(g.grupoId(),o);});
		var escolhidas=new HashSet<UUID>();
		if(r.porcoes()!=null)for(var p:r.porcoes()){
			var opcoes=catalogo.get(p.grupoId());
			if(opcoes==null||!opcoes.containsKey(p.opcao())||!escolhidas.add(p.grupoId()))throw new ExcecaoNegocio(CodigoErro.PORCAO_INVALIDA,"RN-DIE-10");
		}
		var principais=new HashSet<UUID>();
		for(var refeicao:r.refeicoes())if(!"ALTERNATIVA".equals(refeicao.tipo())&&refeicao.id()!=null)principais.add(refeicao.id());
		var usados=new HashSet<UUID>();var idsInformados=new HashSet<UUID>();
		for(var refeicao:r.refeicoes()){
			if(refeicao.itens().size()>MAX_ITENS)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-DIE-01");
			unico(refeicao.id(),idsInformados);
			boolean alternativa="ALTERNATIVA".equals(refeicao.tipo());
			if(alternativa!=(refeicao.alternativaDe()!=null)||(alternativa&&!principais.contains(refeicao.alternativaDe())))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-DIE-11");
			for(var item:refeicao.itens()){
				unico(item.id(),idsInformados);
				boolean temTexto=item.textoLivre()!=null&&!item.textoLivre().isBlank();
				int origens=(item.alimentoId()!=null?1:0)+(item.grupoId()!=null?1:0);
				if(origens>1||(origens==0&&!(Boolean.TRUE.equals(item.aVontade())&&temTexto))||(origens==1&&temTexto)||(Boolean.TRUE.equals(item.aVontade())&&item.porcoes()!=null)||(!Boolean.TRUE.equals(item.aVontade())&&origens==1&&item.porcoes()==null))
					throw new ExcecaoNegocio(CodigoErro.ITEM_INVALIDO,"RN-DIE-10");
				if(item.alimentoId()!=null)usados.add(item.alimentoId());
				if(item.grupoId()!=null&&(!catalogo.containsKey(item.grupoId())||(!Boolean.TRUE.equals(item.aVontade())&&!escolhidas.contains(item.grupoId()))))throw new ExcecaoNegocio(CodigoErro.PORCAO_INVALIDA,"RN-DIE-10");
				var p=item.porcoes();if(p!=null&&(p.stripTrailingZeros().scale()>4||p.compareTo(new BigDecimal("1000000"))>=0))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-DIE-05");
			}
		}
		var encontrados=alimentos.consultar(nutri,usados);
		if(!encontrados.keySet().containsAll(usados))throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		for(var a:encontrados.values())if(!a.ativo())throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-ALI-03");
		var extras=new DietaStore.Extras(r.porcoes()==null||r.porcoes().isEmpty()?null:mapper.writeValueAsString(r.porcoes()),r.metas()==null?null:mapper.writeValueAsString(r.metas()),null,texto(r.rodape()));
		return new Conteudo(extras);
	}
	private void unico(UUID id,Set<UUID> vistos){if(id!=null&&(id.version()!=7||!vistos.add(id)))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);}
	private void gravar(UUID dieta,SalvarDietaRequisicao r,Conteudo conteudo){
		for(var refeicao:r.refeicoes()){
			UUID rid=refeicao.id()==null?ids.novo():refeicao.id();
			String tipo=refeicao.tipo()==null?"PRINCIPAL":refeicao.tipo();
			if(!dietas.inserirRefeicao(new Refeicao(rid,dieta,refeicao.nome().strip(),refeicao.horario(),refeicao.ordem(),tipo,refeicao.alternativaDe())))throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02");
			int ordem=0;
			for(var item:refeicao.itens()){
				UUID iid=item.id()==null?ids.novo():item.id();
				if(!dietas.inserirItem(new Item(iid,rid,item.alimentoId(),item.grupoId(),item.porcoes(),Boolean.TRUE.equals(item.aVontade()),texto(item.textoLivre()),texto(item.observacao()),ordem++)))throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02");
			}
		}
	}
	private Dieta propria(UUID id,UUID nutri,boolean bloquear){
		var dieta=dietas.dieta(id,nutri,bloquear);if(dieta==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);return dieta;
	}
	private UUID idV7(UUID id){if(id.version()!=7)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);return id;}
	private String texto(String valor){return valor==null||valor.isBlank()?null:valor.strip();}
	private ReferenciaClinica referencia(Dieta dieta,ContextoUsuario contexto){return new ReferenciaClinica("dieta",dieta.id(),contexto.exigirNutricionista(),dieta.pacienteId(),contexto.organizacaoId());}
}
