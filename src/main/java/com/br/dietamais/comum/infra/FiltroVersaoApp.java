package com.br.dietamais.comum.infra;

import com.br.dietamais.comum.api.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Versão mínima do app (integracao.md): se o cliente manda {@code X-App-Versao} menor que {@code dieta.app.versao-minima}, responde 426
 * {@code VERSAO_APP_OBSOLETA} para o app mostrar a tela de atualização. Sem o cabeçalho (site, curl, links públicos) não bloqueia.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE+30)
public class FiltroVersaoApp extends OncePerRequestFilter {
	private static final Pattern VERSAO=Pattern.compile("^v?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:[-+].*)?$");
	private final long[] minima;private final long[] minimaWeb;private final FabricaProblema problemas;
	public FiltroVersaoApp(FabricaProblema problemas,@Value("${dieta.app.versao-minima:}") String minima,
			@Value("${dieta.app.versao-minima-web:}") String minimaWeb){
		this.problemas=problemas;this.minima=minima.isBlank()?null:ler(minima.strip());
		if(!minima.isBlank()&&this.minima==null)throw new IllegalStateException("APP_VERSAO_MINIMA inválida; use o formato 1.2.3");
		this.minimaWeb=minimaWeb.isBlank()?null:ler(minimaWeb.strip());
		if(!minimaWeb.isBlank()&&this.minimaWeb==null)throw new IllegalStateException("APP_VERSAO_MINIMA_WEB inválida; use o formato 1.2.3");
	}
	@Override protected boolean shouldNotFilter(HttpServletRequest req){
		String c=req.getRequestURI();
		return (minima==null&&minimaWeb==null)||!c.startsWith("/api/v1/")||c.startsWith("/api/v1/webhooks/")||c.startsWith("/api/v1/arquivos/")||c.startsWith("/api/v1/exportacoes/");
	}
	@Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain cadeia) throws ServletException,IOException {
		var informada=req.getHeader("X-App-Versao");
		var versao=informada==null?null:ler(informada.strip());
		// BE-37: o site tem versão mínima própria (X-Plataforma: WEB); sem ela, WEB não é bloqueado pela dos apps
		var exigida="WEB".equalsIgnoreCase(req.getHeader("X-Plataforma"))?minimaWeb:minima;
		if(versao!=null&&exigida!=null&&comparar(versao,exigida)<0){
			res.setHeader("Cache-Control","no-store");
			problemas.escrever(res,CodigoErro.VERSAO_APP_OBSOLETA,"Esta versão do aplicativo não é mais aceita. Atualize para continuar.");return;
		}
		cadeia.doFilter(req,res);
	}
	static long[] ler(String texto){
		var m=VERSAO.matcher(texto);if(!m.matches())return null;
		var v=new long[3];
		try{for(int i=0;i<3;i++)v[i]=m.group(i+1)==null?0:Long.parseLong(m.group(i+1));}catch(NumberFormatException erro){return null;}
		return v;
	}
	static int comparar(long[] a,long[] b){for(int i=0;i<3;i++)if(a[i]!=b[i])return Long.compare(a[i],b[i]);return 0;}
}
