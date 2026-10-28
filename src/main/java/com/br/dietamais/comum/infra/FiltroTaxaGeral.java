package com.br.dietamais.comum.infra;

import com.br.dietamais.comum.api.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Limite geral da API: 300 requisições por minuto por usuário autenticado (integracao.md). Excedeu: 429 com Retry-After. */
@Component
@Order(-96) // depois do Security (autenticou), antes do contexto do usuário (que consulta o banco)
public class FiltroTaxaGeral extends OncePerRequestFilter {
	private final JanelaTaxa janela;private final Clock relogio;private final FabricaProblema problemas;
	public FiltroTaxaGeral(Clock relogio,FabricaProblema problemas,@Value("${dieta.taxa.usuario-por-minuto:300}") int limite){
		this.relogio=relogio;this.problemas=problemas;this.janela=limite>0?new JanelaTaxa(limite,50_000):null;
	}
	@Override protected boolean shouldNotFilter(HttpServletRequest req){return janela==null;}
	@Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain cadeia) throws ServletException,IOException {
		var autenticacao=SecurityContextHolder.getContext().getAuthentication();
		if(autenticacao!=null&&autenticacao.isAuthenticated()&&!(autenticacao instanceof AnonymousAuthenticationToken)&&autenticacao.getPrincipal() instanceof Jwt jwt&&jwt.getSubject()!=null){
			long espera=janela.espera(jwt.getSubject(),relogio.instant());
			if(espera>0){
				res.setHeader("Retry-After",Long.toString(espera));res.setHeader("Cache-Control","no-store");
				problemas.escrever(res,CodigoErro.TAXA_EXCEDIDA,"Muitas requisições. Aguarde um pouco e tente de novo.");return;
			}
		}
		cadeia.doFilter(req,res);
	}
}
