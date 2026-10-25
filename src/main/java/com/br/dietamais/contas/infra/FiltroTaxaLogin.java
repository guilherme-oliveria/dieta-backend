package com.br.dietamais.contas.infra;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.dominio.LimiteTaxaLogin;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE+20)
public class FiltroTaxaLogin extends OncePerRequestFilter {
	private final LimiteTaxaLogin limite;
	private final Clock relogio;
	private final FabricaProblema problemas;
	public FiltroTaxaLogin(Clock relogio,FabricaProblema problemas,@Value("${dieta.auth.login-limite-ip:10}") int quantidade) {
		this.relogio=relogio;this.problemas=problemas;limite=new LimiteTaxaLogin(quantidade,10000);
	}
	@Override protected boolean shouldNotFilter(HttpServletRequest req) {
		return !"POST".equals(req.getMethod())||!"/api/v1/auth/login".equals(req.getRequestURI());
	}
	@Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain cadeia) throws IOException,ServletException {
		long espera=limite.espera(req.getRemoteAddr(),relogio.instant());
		if(espera>0) {
			res.setHeader("Retry-After",Long.toString(espera));res.setHeader("Cache-Control","no-store");
			problemas.escrever(res,CodigoErro.TAXA_EXCEDIDA,"Aguarde antes de tentar novamente.");return;
		}
		cadeia.doFilter(req,res);
	}
}
