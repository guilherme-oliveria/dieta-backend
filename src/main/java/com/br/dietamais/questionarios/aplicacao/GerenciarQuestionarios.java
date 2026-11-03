package com.br.dietamais.questionarios.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.*;
import com.br.dietamais.questionarios.api.*;
import com.br.dietamais.questionarios.dominio.ValidadorResposta;
import com.br.dietamais.questionarios.infra.QuestionarioStore;
import java.time.Clock;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.JsonNode;

/** Envio (nutri) e resposta (paciente) de questionários: RN-ANA-03 (imutável depois de enviado) e RN-ANA-04 (perguntas congeladas). */
@Service
public class GerenciarQuestionarios {
	/** Teto do conjunto de respostas de um questionário, para a linha não crescer sem limite. */
	static final int MAX_BYTES_RESPOSTAS=64*1024;
	private final QuestionarioStore store;private final ConsultarFichaParaLista fichas;private final UsuarioAtual usuario;private final GuardaEscritaNutri guarda;
	private final AuditoriaClinica auditoria;private final GeradorIds ids;private final Clock relogio;private final ApplicationEventPublisher eventos;
	public GerenciarQuestionarios(QuestionarioStore store,ConsultarFichaParaLista fichas,UsuarioAtual usuario,GuardaEscritaNutri guarda,AuditoriaClinica auditoria,GeradorIds ids,Clock relogio,ApplicationEventPublisher eventos){
		this.store=store;this.fichas=fichas;this.usuario=usuario;this.guarda=guarda;this.auditoria=auditoria;this.ids=ids;this.relogio=relogio;this.eventos=eventos;
	}

	// ---- nutri ----
	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public QuestionariosResposta listarDoPaciente(UUID paciente){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();var ficha=fichas.consultar(paciente,false);
		var itens=store.porVinculo(ficha.vinculoId(),nutri);
		auditoria.leitura(new ReferenciaClinica("questionario",ficha.vinculoId(),nutri,ficha.pacienteId(),contexto.organizacaoId()));
		return new QuestionariosResposta(itens);
	}

	@Transactional
	public QuestionarioResposta enviar(UUID paciente,EnviarQuestionarioRequisicao r){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();guarda.validar();store.bloquear(nutri);
		var ficha=fichas.consultar(paciente,true);
		if(ficha.status()!=StatusVinculo.ATIVO)throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-VIN-05");
		var modelo=store.modelo(r.modeloId(),nutri);if(modelo==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		UUID id=r.id()==null?ids.novo():r.id();
		if(r.id()!=null&&r.id().version()!=7)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		// RN-ANA-04: copia as perguntas do modelo neste instante; editar o modelo depois não muda o que o paciente recebeu
		if(!store.inserirQuestionario(id,ficha.vinculoId(),nutri,ficha.pacienteId(),modelo.id(),modelo.nome(),modelo.blocos(),relogio.instant()))throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02");
		var criado=store.daNutri(id,nutri);auditoria.escrita(referencia(criado,contexto));
		eventos.publishEvent(new QuestionarioEnviado(id,nutri,ficha.pacienteId()));
		return criado;
	}

	// ---- paciente ----
	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public QuestionariosResposta meus(){return new QuestionariosResposta(store.doPaciente(usuario.obter().exigirPaciente()));}

	/** RN-ANA-03: pode salvar parcial quantas vezes quiser até enviar; depois disso 409 {@code QUESTIONARIO_FECHADO}. */
	@Transactional
	public QuestionarioResposta salvarRespostas(UUID id,SalvarRespostasRequisicao r){
		var paciente=usuario.obter().exigirPaciente();var estado=abrir(id,paciente);
		if(r.respostas().isEmpty())return store.doPacienteUm(id,paciente);
		var perguntas=ValidadorResposta.indice(estado.blocos());var gravar=new LinkedHashMap<UUID,JsonNode>();var apagar=new LinkedHashSet<UUID>();
		for(var e:r.respostas().entrySet()){
			var pergunta=perguntas.get(e.getKey());
			if(pergunta==null)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-ANA-04","Pergunta inexistente neste questionário.",
				List.of(new ErroCampoResposta(e.getKey().toString(),"FORMATO_INVALIDO","Pergunta inexistente neste questionário.")),null);
			if(e.getValue()==null||e.getValue().isNull())apagar.add(e.getKey());
			else{ValidadorResposta.validar(pergunta,e.getValue());gravar.put(e.getKey(),e.getValue());}
		}
		var atuais=new LinkedHashMap<>(store.respostasDe(id));atuais.putAll(gravar);apagar.forEach(atuais::remove);
		if(tamanho(atuais)>MAX_BYTES_RESPOSTAS)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-ANA-02");
		store.mesclarRespostas(id,gravar,apagar,relogio.instant());
		return store.doPacienteUm(id,paciente);
	}

	@Transactional
	public QuestionarioResposta concluir(UUID id){
		var paciente=usuario.obter().exigirPaciente();var estado=abrir(id,paciente);
		var respostas=store.respostasDe(id);var faltando=new ArrayList<ErroCampoResposta>();
		for(var p:ValidadorResposta.indice(estado.blocos()).values())
			if(Boolean.TRUE.equals(p.obrigatoria())&&ValidadorResposta.visivel(p,respostas)&&!respostas.containsKey(p.id()))
				faltando.add(new ErroCampoResposta(p.id().toString(),"OBRIGATORIO","Responda esta pergunta."));
		if(!faltando.isEmpty())throw new ExcecaoNegocio(CodigoErro.OBRIGATORIO,"RN-ANA-03","Há perguntas obrigatórias sem resposta.",faltando,null);
		store.marcarRespondido(id,relogio.instant());
		eventos.publishEvent(new QuestionarioRespondido(id,estado.nutricionistaId(),paciente));
		return store.doPacienteUm(id,paciente);
	}

	/** Bloqueia a linha e confere dono, vínculo ativo (RN-VIN-05) e se ainda aceita respostas. */
	private QuestionarioStore.Estado abrir(UUID id,UUID paciente){
		var estado=store.estadoBloqueado(id);
		if(estado==null||!estado.pacienteId().equals(paciente))throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		if(estado.status()==StatusQuestionario.RESPONDIDO)throw new ExcecaoNegocio(CodigoErro.QUESTIONARIO_FECHADO,"RN-ANA-03");
		if(store.doPacienteUm(id,paciente)==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		if(!vinculoAtivo(estado.vinculoId(),paciente))throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-VIN-05");
		return estado;
	}
	private boolean vinculoAtivo(UUID vinculo,UUID paciente){return store.vinculoAtivo(vinculo,paciente);}
	private static int tamanho(Map<UUID,JsonNode> m){return m.values().stream().mapToInt(v->v.toString().length()).sum();}
	private ReferenciaClinica referencia(QuestionarioResposta q,ContextoUsuario contexto){return new ReferenciaClinica("questionario",q.id(),contexto.exigirNutricionista(),q.pacienteId(),contexto.organizacaoId());}
}
