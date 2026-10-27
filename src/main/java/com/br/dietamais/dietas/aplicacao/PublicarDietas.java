package com.br.dietamais.dietas.aplicacao;

import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.dietas.dominio.*;
import com.br.dietamais.dietas.dominio.LinhasDieta.*;
import com.br.dietamais.dietas.infra.DietaStore;
import com.br.dietamais.pacientes.api.*;
import java.time.Clock;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PublicarDietas {
	private final MontadorDieta montador;private final DietaStore dietas;private final ConsultarAlimentosParaDieta alimentos;private final ListaSubstituicaoEfetiva listas;
	private final ConsultaNutricionistas nutricionistas;private final ConsultarFichaParaLista fichas;private final UsuarioAtual usuario;private final GuardaEscritaNutri guarda;
	private final AuditoriaClinica auditoria;private final GeradorIds ids;private final Clock relogio;private final JsonMapper mapper;private final ApplicationEventPublisher eventos;
	public PublicarDietas(MontadorDieta montador,DietaStore dietas,ConsultarAlimentosParaDieta alimentos,ListaSubstituicaoEfetiva listas,ConsultaNutricionistas nutricionistas,ConsultarFichaParaLista fichas,
		UsuarioAtual usuario,GuardaEscritaNutri guarda,AuditoriaClinica auditoria,GeradorIds ids,Clock relogio,JsonMapper mapper,ApplicationEventPublisher eventos){
		this.montador=montador;this.dietas=dietas;this.alimentos=alimentos;this.listas=listas;this.nutricionistas=nutricionistas;this.fichas=fichas;this.usuario=usuario;this.guarda=guarda;
		this.auditoria=auditoria;this.ids=ids;this.relogio=relogio;this.mapper=mapper;this.eventos=eventos;
	}
	/** RN-DIE-02/03/06/09: congela dieta, lista de substituição e orientação; a publicada anterior do vínculo vira SUBSTITUIDA. */
	@Transactional
	public DietaSnapshotResposta publicar(UUID id,String ifMatch){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();guarda.validar();dietas.bloquear(nutri);
		var atual=propria(id,nutri,true);
		if(atual.status()!=StatusDieta.RASCUNHO)throw new ExcecaoNegocio(CodigoErro.DIETA_NAO_EDITAVEL,"RN-DIE-04");
		var ficha=fichas.consultar(atual.pacienteId(),true);
		if(ficha.status()==StatusVinculo.ENCERRADO)throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-VIN-05");
		var resposta=montador.montar(List.of(atual),nutri).get(0);
		if(VersaoDieta.ler(ifMatch)!=atual.versao()){
			auditoria.leitura(referencia(atual,contexto));
			throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02","A dieta foi atualizada. Revise os dados atuais.",List.of(),mapper.valueToTree(resposta));
		}
		var refeicoes=resposta.refeicoes().stream().filter(r->!r.itens().isEmpty()).toList();
		if(refeicoes.isEmpty())throw new ExcecaoNegocio(CodigoErro.DIETA_VAZIA,"RN-DIE-06");
		var usados=new HashSet<UUID>();refeicoes.forEach(r->r.itens().forEach(i->usados.add(i.alimentoId())));
		for(var a:alimentos.consultar(nutri,usados).values())if(!a.ativo())throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-ALI-03");
		var agora=relogio.instant();var resumo=nutricionistas.porIds(Set.of(nutri)).get(nutri);
		if(resumo==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		String orientacao=atual.orientacao()!=null?atual.orientacao():nutricionistas.orientacaoPadrao(nutri).orElse(null);
		var lista=listas.consultar(atual.vinculoId(),atual.pacienteId(),nutri);
		var snapshot=new DietaSnapshotResposta(ids.novo(),atual.id(),atual.pacienteId(),atual.vinculoId(),new NutricionistaResumoResposta(nutri,resumo.nome(),resumo.crn()),
			atual.titulo(),orientacao,refeicoes,lista,CalculadoraTotais.somar(refeicoes.stream().map(RefeicaoResposta::totais).toList()),agora);
		dietas.substituirPublicada(atual.vinculoId(),atual.id(),agora);dietas.marcarPublicada(atual.id(),nutri,agora);
		dietas.inserirSnapshot(snapshot.id(),atual.id(),atual.vinculoId(),nutri,atual.pacienteId(),mapper.writeValueAsString(snapshot),agora);
		auditoria.escrita(referencia(atual,contexto));
		eventos.publishEvent(new DietaPublicada(snapshot.id(),atual.id(),atual.vinculoId(),atual.pacienteId(),nutri,agora));
		return snapshot;
	}
	/** RN-DIE-04: editar uma dieta publicada é duplicar em novo rascunho. */
	@Transactional
	public DietaResposta duplicar(UUID id){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();guarda.validar();dietas.bloquear(nutri);
		var origem=propria(id,nutri,false);var ficha=fichas.consultar(origem.pacienteId(),true);
		if(ficha.status()==StatusVinculo.ENCERRADO)throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-VIN-05");
		var agora=relogio.instant();UUID novo=ids.novo();String titulo=origem.titulo();titulo=(titulo.length()>141?titulo.substring(0,141):titulo)+" (cópia)";
		dietas.inserir(novo,origem.vinculoId(),nutri,origem.pacienteId(),titulo,origem.orientacao(),agora);
		var refeicoes=dietas.refeicoes(List.of(id));var itens=dietas.itens(refeicoes.stream().map(Refeicao::id).toList());
		for(var r:refeicoes){
			UUID rid=ids.novo();dietas.inserirRefeicao(new Refeicao(rid,novo,r.nome(),r.horario(),r.ordem()));
			for(var i:itens)if(i.refeicaoId().equals(r.id()))dietas.inserirItem(new Item(ids.novo(),rid,i.alimentoId(),i.porcoes(),i.observacao(),i.ordem()));
		}
		var copia=propria(novo,nutri,false);var resposta=montador.montar(List.of(copia),nutri).get(0);auditoria.escrita(referencia(copia,contexto));return resposta;
	}
	private Dieta propria(UUID id,UUID nutri,boolean bloquear){
		var dieta=dietas.dieta(id,nutri,bloquear);if(dieta==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);return dieta;
	}
	private ReferenciaClinica referencia(Dieta dieta,ContextoUsuario contexto){return new ReferenciaClinica("dieta",dieta.id(),contexto.exigirNutricionista(),dieta.pacienteId(),contexto.organizacaoId());}
}
