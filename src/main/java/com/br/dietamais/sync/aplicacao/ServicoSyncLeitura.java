package com.br.dietamais.sync.aplicacao;

import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.avaliacoes.api.AvaliacaoResposta;
import com.br.dietamais.questionarios.api.ModeloQuestionarioResposta;
import com.br.dietamais.questionarios.api.QuestionarioResposta;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.rotina.api.*;
import com.br.dietamais.pacientes.api.*;
import com.br.dietamais.sync.api.*;
import com.br.dietamais.sync.dominio.CursorSync;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ServicoSyncLeitura {
	private final UsuarioAtual usuario;private final DiarioSync diario;private final List<FonteSync<?>> fontes;
	public ServicoSyncLeitura(UsuarioAtual usuario,DiarioSync diario,List<FonteSync<?>> fontes) {
		this.usuario=usuario;this.diario=diario;this.fontes=List.copyOf(fontes);
		var colecoes=EnumSet.noneOf(ColecaoSync.class);
		for(var fonte:fontes)if(fonte.colecao()==null||!colecoes.add(fonte.colecao()))throw new IllegalArgumentException("Fonte sync inválida ou duplicada");
		if(colecoes.size()!=ColecaoSync.values().length)throw new IllegalArgumentException("Fontes sync ausentes");
	}
	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public SyncResposta executar(String valor,int limite) {
		var contexto=usuario.obter();if(contexto.perfil()==Perfil.ADMIN)throw new ExcecaoNegocio(CodigoErro.PERFIL_INVALIDO,null);
		long cursor=CursorSync.ler(valor),marco=diario.marcoAtual();CursorSync.validar(cursor,marco,limite);
		var candidatos=new ArrayList<Entrada>();var sequencias=new HashSet<Long>();
		for(var fonte:fontes)for(var referencia:fonte.mudancasDesde(cursor,contexto,limite+1)) {
			if(referencia.seq()<=cursor||referencia.seq()>marco||!sequencias.add(referencia.seq()))throw new IllegalStateException("Revisão sync inconsistente");
			candidatos.add(new Entrada(fonte,referencia));
		}
		candidatos.sort(Comparator.comparingLong(entrada->entrada.referencia().seq()));boolean mais=candidatos.size()>limite;
		var pacientes=new ArrayList<MudancaSyncResposta<PacienteResposta>>();var vinculos=new ArrayList<MudancaSyncResposta<VinculoResposta>>();var nutris=new ArrayList<MudancaSyncResposta<NutricionistaResumoResposta>>();
		var grupos=new ArrayList<MudancaSyncResposta<GrupoAlimentoResposta>>();var alimentos=new ArrayList<MudancaSyncResposta<AlimentoResposta>>();var listas=new ArrayList<MudancaSyncResposta<ListaSubstituicaoResposta>>();
		var dietas=new ArrayList<MudancaSyncResposta<DietaResposta>>();var snapshots=new ArrayList<MudancaSyncResposta<DietaSnapshotResposta>>();var emUso=new ArrayList<MudancaSyncResposta<DietaEmUsoResposta>>();
		var checkins=new ArrayList<MudancaSyncResposta<CheckinResposta>>();var agua=new ArrayList<MudancaSyncResposta<AguaResposta>>();var pesos=new ArrayList<MudancaSyncResposta<PesoResposta>>();var avaliacoes=new ArrayList<MudancaSyncResposta<AvaliacaoResposta>>();var modelos=new ArrayList<MudancaSyncResposta<ModeloQuestionarioResposta>>();var questionarios=new ArrayList<MudancaSyncResposta<QuestionarioResposta>>();
		long ultimo=cursor;
		for(var entrada:candidatos.subList(0,Math.min(limite,candidatos.size()))) {
			var referencia=entrada.referencia();Object dados=entrada.fonte().carregar(referencia,contexto);ultimo=referencia.seq();
			switch(entrada.fonte().colecao()) {
				case PACIENTES->pacientes.add(mudanca(referencia,dados,PacienteResposta.class));
				case VINCULOS->vinculos.add(mudanca(referencia,dados,VinculoResposta.class));
				case NUTRIS->nutris.add(mudanca(referencia,dados,NutricionistaResumoResposta.class));
				case GRUPOS->grupos.add(mudanca(referencia,dados,GrupoAlimentoResposta.class));
				case ALIMENTOS->alimentos.add(mudanca(referencia,dados,AlimentoResposta.class));
				case LISTAS->listas.add(mudanca(referencia,dados,ListaSubstituicaoResposta.class));
				case DIETAS->dietas.add(mudanca(referencia,dados,DietaResposta.class));
				case SNAPSHOTS->snapshots.add(mudanca(referencia,dados,DietaSnapshotResposta.class));
				case DIETA_EM_USO->emUso.add(mudanca(referencia,dados,DietaEmUsoResposta.class));
				case CHECKINS->checkins.add(mudanca(referencia,dados,CheckinResposta.class));
				case AGUA->agua.add(mudanca(referencia,dados,AguaResposta.class));
				case PESOS->pesos.add(mudanca(referencia,dados,PesoResposta.class));
				case AVALIACOES->avaliacoes.add(mudanca(referencia,dados,AvaliacaoResposta.class));
				case MODELOS->modelos.add(mudanca(referencia,dados,ModeloQuestionarioResposta.class));
				case QUESTIONARIOS->questionarios.add(mudanca(referencia,dados,QuestionarioResposta.class));
			}
		}
		return new SyncResposta(Long.toString(mais?ultimo:marco),mais,new MudancasFase1Resposta(pacientes,vinculos,nutris,grupos,alimentos,listas,dietas,snapshots,emUso,checkins,agua,pesos,avaliacoes,modelos,questionarios));
	}
	private <T> MudancaSyncResposta<T> mudanca(ReferenciaSync referencia,Object dados,Class<T> tipo){return new MudancaSyncResposta<>(referencia.id(),referencia.seq(),dados==null,tipo.cast(dados));}
	private record Entrada(FonteSync<?> fonte,ReferenciaSync referencia){}
}
