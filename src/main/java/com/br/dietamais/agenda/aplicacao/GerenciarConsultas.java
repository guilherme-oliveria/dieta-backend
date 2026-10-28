package com.br.dietamais.agenda.aplicacao;

import com.br.dietamais.agenda.api.*;
import com.br.dietamais.agenda.infra.ConsultaStore;
import com.br.dietamais.agenda.infra.ConsultaStore.Dados;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

/** RN-AGE-01: a nutri agenda, remarca e cancela consultas de pacientes vinculados; o paciente só lê. */
@Service
public class GerenciarConsultas {
	/** Tolerância para relógios desajustados no aparelho (mesma das avaliações). */
	static final Duration TOLERANCIA=Duration.ofMinutes(5);
	static final Duration HORIZONTE=Duration.ofDays(730);
	static final Duration JANELA_MAXIMA=Duration.ofDays(366);
	private final ConsultaStore consultas;private final ConsultarFichaParaLista fichas;private final UsuarioAtual usuario;private final GuardaEscritaNutri guarda;
	private final AuditoriaClinica auditoria;private final GeradorIds ids;private final Clock relogio;private final JsonMapper mapper;
	public GerenciarConsultas(ConsultaStore consultas,ConsultarFichaParaLista fichas,UsuarioAtual usuario,GuardaEscritaNutri guarda,AuditoriaClinica auditoria,GeradorIds ids,Clock relogio,JsonMapper mapper){
		this.consultas=consultas;this.fichas=fichas;this.usuario=usuario;this.guarda=guarda;this.auditoria=auditoria;this.ids=ids;this.relogio=relogio;this.mapper=mapper;
	}

	/** Janela padrão: de ontem até 90 dias adiante. {@code paciente} (id da ficha) restringe a um paciente e confere a posse. */
	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public ConsultasResposta listar(UUID paciente,Instant de,Instant ate){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();var janela=janela(de,ate);
		UUID filtro=null;
		if(paciente!=null){
			var ficha=fichas.consultar(paciente,false);filtro=ficha.pacienteId();
			auditoria.leitura(new ReferenciaClinica("consulta",ficha.vinculoId(),nutri,ficha.pacienteId(),contexto.organizacaoId()));
		}
		return new ConsultasResposta(consultas.daNutri(nutri,filtro,janela[0],janela[1]));
	}

	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public ConsultasResposta minhas(Instant de,Instant ate){
		var paciente=usuario.obter().exigirPaciente();var janela=janela(de,ate);
		return new ConsultasResposta(consultas.doPaciente(paciente,janela[0],janela[1]));
	}

	@Transactional
	public ConsultaResposta criar(CriarConsultaRequisicao r){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();guarda.validar();consultas.bloquear(nutri);
		var ficha=fichas.consultar(r.pacienteId(),true);
		if(ficha.status()==StatusVinculo.ENCERRADO)throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-VIN-05");
		var agora=relogio.instant();exigirDataValida(r.inicio(),agora);
		UUID id=r.id()==null?ids.novo():idV7(r.id());
		var dados=new Dados(r.inicio(),r.duracaoMin(),r.tipo(),limpar(r.lugar()),StatusConsulta.AGENDADA);
		if(!consultas.inserir(id,ficha.vinculoId(),nutri,ficha.pacienteId(),dados,agora))throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02");
		var criada=propria(id,nutri,false);auditoria.escrita(referencia(criada,contexto));return criada;
	}

	@Transactional
	public ConsultaResposta atualizar(UUID id,String ifMatch,AtualizarConsultaRequisicao r){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();guarda.validar();consultas.bloquear(nutri);
		var atual=propria(id,nutri,true);
		var ficha=fichas.consultar(atual.pacienteId(),true);
		if(ficha.status()==StatusVinculo.ENCERRADO)throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-VIN-05");
		if(versao(ifMatch)!=atual.versao()){
			auditoria.leitura(referencia(atual,contexto));
			throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02","A consulta foi atualizada. Revise os dados atuais.",List.of(),mapper.valueToTree(atual));
		}
		if(atual.status()==StatusConsulta.CANCELADA)throw new ExcecaoNegocio(CodigoErro.CONSULTA_CANCELADA,"RN-AGE-01");
		boolean mudaCampos=r.inicio()!=null||r.duracaoMin()!=null||r.tipo()!=null||r.lugar()!=null;
		if(r.status()!=null&&(r.status()!=StatusConsulta.CANCELADA||mudaCampos))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-AGE-01");
		if(r.status()==null&&!mudaCampos)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-AGE-01");
		var agora=relogio.instant();
		if(r.inicio()!=null)exigirDataValida(r.inicio(),agora);
		var dados=new Dados(r.inicio()!=null?r.inicio():atual.inicio(),r.duracaoMin()!=null?r.duracaoMin():atual.duracaoMin(),r.tipo()!=null?r.tipo():atual.tipo(),
			r.lugar()!=null?limpar(r.lugar()):atual.lugar(),r.status()!=null?r.status():atual.status());
		consultas.atualizar(id,nutri,dados,agora);
		var nova=propria(id,nutri,false);auditoria.escrita(referencia(nova,contexto));return nova;
	}

	/** [de, ate): padrão de ontem até 90 dias adiante; no máximo 366 dias por consulta. */
	private Instant[] janela(Instant de,Instant ate){
		var inicio=de!=null?de:relogio.instant().minus(Duration.ofDays(1));
		var fim=ate!=null?ate:inicio.plus(Duration.ofDays(90));
		if(!fim.isAfter(inicio)||Duration.between(inicio,fim).compareTo(JANELA_MAXIMA)>0)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-AGE-01");
		return new Instant[]{inicio,fim};
	}
	private ConsultaResposta propria(UUID id,UUID nutri,boolean bloquear){
		var c=consultas.daNutri(id,nutri,bloquear);if(c==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);return c;
	}
	private void exigirDataValida(Instant inicio,Instant agora){
		if(inicio.isBefore(agora.minus(TOLERANCIA))||inicio.isAfter(agora.plus(HORIZONTE)))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-AGE-01");
	}
	private static String limpar(String lugar){return lugar==null||lugar.isBlank()?null:lugar.strip();}
	private UUID idV7(UUID id){if(id.version()!=7)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null);return id;}
	private static long versao(String valor){
		if(valor!=null&&valor.matches("\"[0-9]+\""))try{return Long.parseLong(valor.substring(1,valor.length()-1));}catch(NumberFormatException ignorada){}
		throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SYN-02");
	}
	private ReferenciaClinica referencia(ConsultaResposta c,ContextoUsuario contexto){return new ReferenciaClinica("consulta",c.id(),contexto.exigirNutricionista(),c.pacienteId(),contexto.organizacaoId());}
}
