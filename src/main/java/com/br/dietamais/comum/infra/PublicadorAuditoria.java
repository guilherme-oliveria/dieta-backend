package com.br.dietamais.comum.infra;

import com.br.dietamais.comum.api.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class PublicadorAuditoria implements AuditoriaClinica {
	private final UsuarioAtual usuario;
	private final ApplicationEventPublisher eventos;
	private final Clock relogio;
	private final HttpServletRequest requisicao;
	public PublicadorAuditoria(UsuarioAtual usuario,ApplicationEventPublisher eventos,Clock relogio,HttpServletRequest requisicao) {
		this.usuario=usuario;this.eventos=eventos;this.relogio=relogio;this.requisicao=requisicao;
	}
	@Override public void leitura(ReferenciaClinica recurso){registrar(recurso,"LEITURA");}
	@Override public void escrita(ReferenciaClinica recurso){registrar(recurso,"ESCRITA");}
	private void registrar(ReferenciaClinica recurso,String acao) {
		var contexto=usuario.obter();
		String ip=requisicao.getRemoteAddr();
		if(ip!=null&&ip.contains("%"))ip=ip.substring(0,ip.indexOf('%'));
		eventos.publishEvent(new AcessoClinico(contexto.usuarioId(),contexto.perfil(),recurso,acao,relogio.instant(),ip,MDC.get("traceId")));
	}
}
