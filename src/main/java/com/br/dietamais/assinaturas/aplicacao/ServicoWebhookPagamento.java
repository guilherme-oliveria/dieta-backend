package com.br.dietamais.assinaturas.aplicacao;

import com.br.dietamais.assinaturas.api.StatusAssinatura;
import com.br.dietamais.assinaturas.aplicacao.GatewayPagamento.TipoEvento;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.notificacoes.api.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RN-ASS-07: o status muda por webhook validado e processado uma única vez por {@code evento_id}. */
@Service
public class ServicoWebhookPagamento {
	private final JdbcTemplate jdbc;private final GatewayPagamento gateway;private final Clock relogio;
	private final ConsultaNutricionistas nutris;private final RegistrarAviso avisos;
	public ServicoWebhookPagamento(JdbcTemplate jdbc,GatewayPagamento gateway,Clock relogio,ConsultaNutricionistas nutris,RegistrarAviso avisos) {
		this.jdbc=jdbc;this.gateway=gateway;this.relogio=relogio;this.nutris=nutris;this.avisos=avisos;
	}

	@Transactional
	public void processar(String token,String corpo) {
		if(!gateway.tokenValido(token))throw new ExcecaoNegocio(CodigoErro.NAO_AUTENTICADO,null);
		GatewayPagamento.Evento evento;
		try { evento=gateway.interpretar(corpo); }
		catch (IllegalArgumentException erro) { throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null,"Evento inválido.",java.util.List.of(),null); }
		var novo=jdbc.update("insert into evento_pagamento(evento_id,tipo,gateway_assinatura_id,resultado) values (?,?,?,'PROCESSANDO') on conflict (evento_id) do nothing",
			evento.eventoId(),evento.tipo().name(),evento.assinaturaGatewayId());
		if(novo==0)return;
		var resultado="IGNORADO";UUID assinaturaId=null;
		if(evento.tipo()!=TipoEvento.IGNORADO&&evento.assinaturaGatewayId()!=null){
			var linhas=jdbc.query("select id,organizacao_id,status,fim_periodo_em from assinatura where gateway_assinatura_id=? for update",
				(rs,n)->new Object[]{rs.getObject("id",UUID.class),rs.getObject("organizacao_id",UUID.class),StatusAssinatura.valueOf(rs.getString("status")),rs.getTimestamp("fim_periodo_em")},evento.assinaturaGatewayId());
			if(linhas.isEmpty())resultado="ASSINATURA_DESCONHECIDA";
			else {
				var l=linhas.getFirst();assinaturaId=(UUID)l[0];
				resultado=aplicar(evento,assinaturaId,(UUID)l[1],(StatusAssinatura)l[2],(Timestamp)l[3])?"APLICADO":"IGNORADO";
			}
		}
		jdbc.update("update evento_pagamento set resultado=?,assinatura_id=? where evento_id=?",resultado,assinaturaId,evento.eventoId());
		LoggerFactory.getLogger(ServicoWebhookPagamento.class).info("Evento de pagamento processado; tipo={} resultado={}",evento.tipo(),resultado);
	}

	private boolean aplicar(GatewayPagamento.Evento e,UUID id,UUID organizacao,StatusAssinatura status,Timestamp fimAtual) {
		var agora=relogio.instant();
		var vencimento=e.vencimento()==null?null:LocalDate.parse(e.vencimento()).atStartOfDay(ZoneOffset.UTC).toInstant();
		switch(e.tipo()) {
			case PAGAMENTO_CONFIRMADO -> {
				var proximo=vencimento==null?agora.atZone(ZoneOffset.UTC).plusMonths(1).toInstant():vencimento.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
				var fim=fimAtual!=null&&status==StatusAssinatura.ATIVA&&fimAtual.toInstant().isAfter(proximo)?fimAtual.toInstant():proximo;
				jdbc.update("update assinatura set status='ATIVA',plano_id=coalesce(plano_pendente_id,plano_id),plano_pendente_id=null,fim_periodo_em=? where id=?",Timestamp.from(fim),id);
				avisar(organizacao,e);return true;
			}
			case PAGAMENTO_ATRASADO -> {
				if(status!=StatusAssinatura.ATIVA)return false;
				jdbc.update("update assinatura set status='EM_ATRASO',fim_periodo_em=? where id=?",Timestamp.from(vencimento==null?agora:vencimento),id);
				avisar(organizacao,e);return true;
			}
			case PAGAMENTO_RECUSADO -> { avisar(organizacao,e);return true; }
			case ASSINATURA_CANCELADA -> {
				if(status==StatusAssinatura.ATIVA||status==StatusAssinatura.EM_ATRASO)
					jdbc.update("update assinatura set status='CANCELADA',gateway_assinatura_id=null,plano_pendente_id=null where id=?",id);
				else jdbc.update("update assinatura set gateway_assinatura_id=null,plano_pendente_id=null where id=?",id);
				if(status==StatusAssinatura.ATIVA||status==StatusAssinatura.EM_ATRASO)avisar(organizacao,e);
				return true;
			}
			default -> { return false; }
		}
	}

	private void avisar(UUID organizacao,GatewayPagamento.Evento e) {
		for(var nutri:nutris.daOrganizacao(organizacao))
			avisos.registrar(new NovoAviso(Perfil.NUTRICIONISTA,nutri,TipoAviso.ASSINATURA,"/assinatura","ASSINATURA:"+e.eventoId()));
	}
}
