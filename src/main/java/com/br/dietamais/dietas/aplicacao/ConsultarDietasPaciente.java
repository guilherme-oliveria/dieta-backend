package com.br.dietamais.dietas.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.dietas.infra.DietaStore;
import com.br.dietamais.dietas.infra.DietaStore.SnapshotRecebido;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

/** Visão do paciente (RN-DIE-03, 07; RN-ASS-05: a assinatura da nutri não restringe a leitura). */
@Service
public class ConsultarDietasPaciente implements com.br.dietamais.dietas.api.ConsultaDietaEmUso {
	private final DietaStore dietas;private final UsuarioAtual usuario;private final ConsultaNutricionistas nutricionistas;private final AuditoriaClinica auditoria;
	private final Clock relogio;private final JsonMapper mapper;
	public ConsultarDietasPaciente(DietaStore dietas,UsuarioAtual usuario,ConsultaNutricionistas nutricionistas,AuditoriaClinica auditoria,Clock relogio,JsonMapper mapper){
		this.dietas=dietas;this.usuario=usuario;this.nutricionistas=nutricionistas;this.auditoria=auditoria;this.relogio=relogio;this.mapper=mapper;
	}
	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public DietasPacienteResposta listar(){
		var contexto=usuario.obter();var paciente=contexto.exigirPaciente();
		var recebidos=dietas.snapshotsDoPaciente(paciente);var itens=new ArrayList<DietaSnapshotResposta>();
		var orgs=nutricionistas.porIds(new HashSet<>(recebidos.stream().map(SnapshotRecebido::nutricionistaId).toList()));
		for(var r:recebidos){
			itens.add(doPaciente(mapper.readValue(r.conteudo(),DietaSnapshotResposta.class),paciente));
			var n=orgs.get(r.nutricionistaId());
			auditoria.leitura(new ReferenciaClinica("dieta_snapshot",r.id(),r.nutricionistaId(),paciente,n==null?null:n.organizacaoId()));
		}
		var selecao=dietas.selecao(paciente,false);
		return new DietasPacienteResposta(emUso(recebidos,selecao),selecao==null?0L:selecao.versao(),itens);
	}
	@Transactional
	public DietaEmUsoResposta selecionar(String ifMatch,UUID dietaId){
		var contexto=usuario.obter();var paciente=contexto.exigirPaciente();dietas.bloquearPaciente(paciente);
		var atual=dietas.selecao(paciente,true);long versao=atual==null?0L:atual.versao();
		var recebidos=dietas.snapshotsDoPaciente(paciente);
		if(recebidos.stream().noneMatch(r->r.dietaId().equals(dietaId)&&candidata(r)))throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		if(com.br.dietamais.dietas.dominio.VersaoDieta.ler(ifMatch)!=versao)
			throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02","A seleção foi atualizada. Revise os dados atuais.",List.of(),mapper.valueToTree(new DietaEmUsoResposta(emUso(recebidos,atual),versao)));
		dietas.gravarSelecao(paciente,dietaId,relogio.instant());
		return new DietaEmUsoResposta(dietaId,dietas.selecao(paciente,false).versao());
	}
	@Override @Transactional(readOnly=true)
	public java.util.Optional<DietaSnapshotResposta> snapshotEmUso(UUID paciente){
		var recebidos=dietas.snapshotsDoPaciente(paciente);var id=emUso(recebidos,dietas.selecao(paciente,false));
		return recebidos.stream().filter(r->r.dietaId().equals(id)).findFirst().map(r->doPaciente(mapper.readValue(r.conteudo(),DietaSnapshotResposta.class),paciente));
	}
	/** Dieta em uso e versão da seleção do paciente autenticado; nulo (tombstone no sync) se não há dieta candidata. */
	@Transactional(readOnly=true)
	public DietaEmUsoResposta emUsoAtual(UUID paciente){
		var selecao=dietas.selecao(paciente,false);var id=emUso(dietas.snapshotsDoPaciente(paciente),selecao);
		return id==null?null:new DietaEmUsoResposta(id,selecao==null?0L:selecao.versao());
	}
	/** O conteúdo congelado guarda o id do paciente da época da publicação; o aceite do convite pode tê-lo substituído pela conta global. */
	public static DietaSnapshotResposta doPaciente(DietaSnapshotResposta s,UUID paciente){
		var l=s.listaSubstituicao();
		return new DietaSnapshotResposta(s.id(),s.dietaId(),paciente,s.vinculoId(),s.nutricionista(),s.titulo(),s.orientacao(),s.refeicoes(),
			new com.br.dietamais.alimentos.api.ListaSubstituicaoResposta(paciente,l.versao(),l.alimentos(),l.adicionados(),l.removidos()),s.totais(),s.publicadoEm(),s.porcoes(),s.metas(),s.pesoReferenciaKg(),s.rodape(),s.restricoes());
	}
	private boolean candidata(SnapshotRecebido r){return r.statusDieta().equals("PUBLICADA")&&r.statusVinculo().equals("ATIVO");}
	/** Escolha explícita enquanto válida (ou a sucessora publicada do mesmo vínculo); senão a publicada mais recente (S9). */
	UUID emUso(List<SnapshotRecebido> recebidos,DietaStore.Selecao selecao){
		if(selecao!=null){
			var escolhida=recebidos.stream().filter(r->r.dietaId().equals(selecao.dietaId())).findFirst();
			if(escolhida.isPresent()){
				if(candidata(escolhida.get()))return selecao.dietaId();
				var sucessora=recebidos.stream().filter(r->r.vinculoId().equals(escolhida.get().vinculoId())&&candidata(r)).findFirst();
				if(sucessora.isPresent())return sucessora.get().dietaId();
			}
		}
		return recebidos.stream().filter(this::candidata).map(SnapshotRecebido::dietaId).findFirst().orElse(null);
	}
}
