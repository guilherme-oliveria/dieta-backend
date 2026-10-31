package com.br.dietamais.avaliacoes.aplicacao;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.dominio.*;
import com.br.dietamais.avaliacoes.infra.AvaliacaoStore;
import com.br.dietamais.avaliacoes.infra.AvaliacaoStore.Dados;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.*;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

@Service
public class GerenciarAvaliacoes {
	/** RN-AVA-04: depois de 30 dias a edição exige justificativa registrada. */
	static final Duration JANELA_EDICAO=Duration.ofDays(30);
	private final AvaliacaoStore avaliacoes;private final ConsultarFichaParaLista fichas;private final UsuarioAtual usuario;private final GuardaEscritaNutri guarda;
	private final AuditoriaClinica auditoria;private final GeradorIds ids;private final Clock relogio;private final JsonMapper mapper;private final CalculoAvaliacao calculo;private final GerenciarProtocolosNutri protocolos;
	public GerenciarAvaliacoes(AvaliacaoStore avaliacoes,ConsultarFichaParaLista fichas,UsuarioAtual usuario,GuardaEscritaNutri guarda,AuditoriaClinica auditoria,GeradorIds ids,Clock relogio,JsonMapper mapper,CalculoAvaliacao calculo,GerenciarProtocolosNutri protocolos){
		this.avaliacoes=avaliacoes;this.fichas=fichas;this.usuario=usuario;this.guarda=guarda;this.auditoria=auditoria;this.ids=ids;this.relogio=relogio;this.mapper=mapper;this.calculo=calculo;this.protocolos=protocolos;
	}

	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public AvaliacoesResposta listar(UUID paciente){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();var ficha=fichas.consultar(paciente,false);
		var itens=avaliacoes.porVinculo(ficha.vinculoId(),nutri);
		auditoria.leitura(new ReferenciaClinica("avaliacao",ficha.vinculoId(),nutri,ficha.pacienteId(),contexto.organizacaoId()));
		return new AvaliacoesResposta(itens);
	}

	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public AvaliacaoResposta consultar(UUID id){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();var a=propria(id,nutri);
		auditoria.leitura(referencia(a,contexto));return a;
	}

	@Transactional
	public AvaliacaoResposta criar(UUID paciente,SalvarAvaliacaoRequisicao r){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();guarda.validar();avaliacoes.bloquear(nutri);
		var ficha=fichas.consultar(paciente,true);
		if(ficha.status()==StatusVinculo.ENCERRADO)throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-VIN-05");
		UUID id=r.id()==null?ids.novo():idV7(r.id());var agora=relogio.instant();
		var dados=dados(r,agora,ficha,protocolos.padrao(nutri));
		if(!avaliacoes.inserir(id,ficha.vinculoId(),nutri,ficha.pacienteId(),dados,agora))throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02");
		var criada=propria(id,nutri);auditoria.escrita(referencia(criada,contexto));return criada;
	}

	@Transactional
	public AvaliacaoResposta atualizar(UUID id,String ifMatch,SalvarAvaliacaoRequisicao r){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();guarda.validar();avaliacoes.bloquear(nutri);
		var atual=avaliacoes.ficha(id,nutri,true);if(atual==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		if(r.id()!=null&&!r.id().equals(id))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);
		var ficha=fichas.consultar(atual.pacienteId(),true);
		if(ficha.status()==StatusVinculo.ENCERRADO)throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-VIN-05");
		if(versao(ifMatch)!=atual.versao()){
			var corrente=propria(id,nutri);auditoria.leitura(referencia(corrente,contexto));
			throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02","A avaliação foi atualizada. Revise os dados atuais.",List.of(),mapper.valueToTree(corrente));
		}
		var agora=relogio.instant();
		String justificativa=r.justificativa()==null||r.justificativa().isBlank()?null:r.justificativa().strip();
		boolean foraDaJanela=Duration.between(atual.criadoEm(),agora).compareTo(JANELA_EDICAO)>0;
		if(foraDaJanela&&justificativa==null)throw new ExcecaoNegocio(CodigoErro.JUSTIFICATIVA_OBRIGATORIA,"RN-AVA-04");
		// RN-CAL-03: sem protocolo na edição, vale o que a avaliação já usava, nunca o padrão atual da nutri
		var guardado=propria(id,nutri).protocolo();
		var dados=dados(r,agora,ficha,guardado!=null?guardado:protocolos.padrao(nutri));
		avaliacoes.atualizar(id,nutri,dados,agora);
		if(foraDaJanela)avaliacoes.registrarJustificativa(id,nutri,atual.versao(),justificativa);
		var nova=propria(id,nutri);auditoria.escrita(referencia(nova,contexto));return nova;
	}

	/** RN-AVA-05: o paciente vê só as compartilhadas. */
	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public AvaliacoesResposta minhas(){
		var paciente=usuario.obter().exigirPaciente();
		return new AvaliacoesResposta(avaliacoes.compartilhadasDoPaciente(paciente));
	}

	private Dados dados(SalvarAvaliacaoRequisicao r,java.time.Instant agora,FichaParaLista ficha,ProtocoloAvaliacao semProtocolo){
		LimitesAvaliacao.validar(r);
		if(r.dataHora().isAfter(agora.plus(Duration.ofMinutes(5))))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-AVA-01");
		var imc=CalculadoraImc.imc(r.pesoKg(),r.estaturaM());
		var calculado=calculo.calcular(r,ficha,r.protocolo()!=null?r.protocolo():semProtocolo,imc);
		return new Dados(r.dataHora(),r.pesoKg(),r.estaturaM(),imc,CalculadoraImc.classificar(imc),r.perimetros()==null?PerimetrosAvaliacao.VAZIO:r.perimetros(),
			r.dobras()==null?DobrasAvaliacao.VAZIO:r.dobras(),r.composicao()==null?ComposicaoAvaliacao.VAZIO:r.composicao(),
			r.parecer()==null||r.parecer().isBlank()?null:r.parecer().strip(),r.compartilhada()==null||r.compartilhada(),
			calculado.faixaEtaria(),calculado.protocolo(),r.complementares(),calculado.resultados(),r.compartilhamento());
	}
	private AvaliacaoResposta propria(UUID id,UUID nutri){
		var a=avaliacoes.daNutri(id,nutri);if(a==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);return a;
	}
	private UUID idV7(UUID id){if(id.version()!=7)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);return id;}
	private static long versao(String valor){
		if(valor!=null&&valor.matches("\"[0-9]+\""))try{return Long.parseLong(valor.substring(1,valor.length()-1));}catch(NumberFormatException ignorada){}
		throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SYN-02");
	}
	private ReferenciaClinica referencia(AvaliacaoResposta a,ContextoUsuario contexto){return new ReferenciaClinica("avaliacao",a.id(),contexto.exigirNutricionista(),a.pacienteId(),contexto.organizacaoId());}
}
