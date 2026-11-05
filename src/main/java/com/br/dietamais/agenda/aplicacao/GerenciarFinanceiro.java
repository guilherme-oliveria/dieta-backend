package com.br.dietamais.agenda.aplicacao;

import com.br.dietamais.agenda.api.*;
import com.br.dietamais.agenda.infra.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

/** BE-65 (G-20), RN-FIN-01: financeiro da consulta, só da nutri, online (fora do sync) e sem chegar ao paciente. */
@Service
public class GerenciarFinanceiro {
	static final Duration JANELA_MAXIMA=Duration.ofDays(366);
	private final FinanceiroStore financeiro;private final ConsultaStore consultas;private final ConsultarFichaParaLista fichas;private final UsuarioAtual usuario;
	private final GuardaEscritaNutri guarda;private final AuditoriaClinica auditoria;private final Clock relogio;private final JsonMapper mapper;
	public GerenciarFinanceiro(FinanceiroStore financeiro,ConsultaStore consultas,ConsultarFichaParaLista fichas,UsuarioAtual usuario,GuardaEscritaNutri guarda,AuditoriaClinica auditoria,Clock relogio,JsonMapper mapper){
		this.financeiro=financeiro;this.consultas=consultas;this.fichas=fichas;this.usuario=usuario;this.guarda=guarda;this.auditoria=auditoria;this.relogio=relogio;this.mapper=mapper;
	}

	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public FinanceiroConsultaResposta consultar(UUID consulta){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();var c=propria(consulta,nutri,false);
		auditoria.leitura(referencia(c,contexto));return atual(consulta,nutri,false);
	}

	@Transactional
	public FinanceiroConsultaResposta atualizar(UUID consulta,String ifMatch,AtualizarFinanceiroConsultaRequisicao r){
		var contexto=usuario.obter();var nutri=contexto.exigirNutricionista();guarda.validar();consultas.bloquear(nutri);
		var c=propria(consulta,nutri,true);
		var ficha=fichas.consultar(c.pacienteId(),true);
		if(ficha.status()==StatusVinculo.ENCERRADO)throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO,"RN-VIN-05");
		if(c.status()==StatusConsulta.CANCELADA)throw new ExcecaoNegocio(CodigoErro.CONSULTA_CANCELADA,"RN-AGE-01");
		var existente=financeiro.da(consulta,nutri,true);long versaoAtual=existente==null?0:existente.versao();
		if(versao(ifMatch)!=versaoAtual){
			auditoria.leitura(referencia(c,contexto));
			throw new ExcecaoNegocio(CodigoErro.CONFLITO_VERSAO,"RN-SYN-02","O financeiro foi atualizado. Revise os dados atuais.",List.of(),mapper.valueToTree(atual(consulta,nutri,false)));
		}
		// RN-FIN-01: marcar como pago exige valor e forma; valor ou forma sem comparecimento da paciente não faz sentido se faltou sem cobrança, mas a nutri pode cobrar falta
		if(r.pago()&&(r.valor()==null||r.formaPagamento()==null))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-FIN-01");
		if(r.pago()&&r.comparecimento()==ComparecimentoConsulta.PENDENTE)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-FIN-01");
		var agora=relogio.instant();
		if(existente==null)financeiro.inserir(consulta,nutri,r,agora);else financeiro.atualizar(consulta,nutri,r,agora);
		var nova=atual(consulta,nutri,false);auditoria.escrita(referencia(c,contexto));return nova;
	}

	@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
	public ResumoFinanceiroResposta resumo(Instant de,Instant ate){
		var nutri=usuario.obter().exigirNutricionista();
		var inicio=de!=null?de:relogio.instant().minus(Duration.ofDays(30));var fim=ate!=null?ate:relogio.instant().plus(Duration.ofDays(1));
		if(!fim.isAfter(inicio)||Duration.between(inicio,fim).compareTo(JANELA_MAXIMA)>0)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-FIN-01");
		return financeiro.resumo(nutri,inicio,fim);
	}

	private FinanceiroConsultaResposta atual(UUID consulta,UUID nutri,boolean bloquear){
		var f=financeiro.da(consulta,nutri,bloquear);
		return f!=null?f:new FinanceiroConsultaResposta(consulta,ComparecimentoConsulta.PENDENTE,null,null,false,0L);
	}
	private ConsultaResposta propria(UUID id,UUID nutri,boolean bloquear){
		var c=consultas.daNutri(id,nutri,bloquear);if(c==null)throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);return c;
	}
	private static long versao(String valor){
		if(valor!=null&&valor.matches("\"[0-9]+\""))try{return Long.parseLong(valor.substring(1,valor.length()-1));}catch(NumberFormatException ignorada){}
		throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-SYN-02");
	}
	private ReferenciaClinica referencia(ConsultaResposta c,ContextoUsuario contexto){return new ReferenciaClinica("consulta_financeiro",c.id(),contexto.exigirNutricionista(),c.pacienteId(),contexto.organizacaoId());}
}
