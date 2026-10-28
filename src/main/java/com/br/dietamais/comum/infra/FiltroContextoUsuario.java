package com.br.dietamais.comum.infra;

import com.br.dietamais.comum.api.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.slf4j.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
@Order(-95) // Security(-100) já autenticou; contexto e plano são conferidos antes da idempotência(-90).
public class FiltroContextoUsuario extends OncePerRequestFilter {
	private final UsuarioAtual usuario;
	private final GuardaEscritaNutri guarda;
	private final FabricaProblema problemas;
	private final ObjectProvider<RequestMappingHandlerMapping> rotas;
	public FiltroContextoUsuario(UsuarioAtual usuario,GuardaEscritaNutri guarda,FabricaProblema problemas,
			@Qualifier("requestMappingHandlerMapping") ObjectProvider<RequestMappingHandlerMapping> rotas) {
		this.usuario=usuario;this.guarda=guarda;this.problemas=problemas;this.rotas=rotas;
	}
	@Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain cadeia) throws ServletException,IOException {
		var autenticacao=SecurityContextHolder.getContext().getAuthentication();
		if(autenticacao!=null&&autenticacao.isAuthenticated()&&!(autenticacao instanceof AnonymousAuthenticationToken)) {
			try { validar(req); }
			catch(ExcecaoNegocio erro) {
				if(erro.codigo()==CodigoErro.NAO_AUTENTICADO)res.setHeader("WWW-Authenticate","Bearer");
				problemas.escrever(res,erro);return;
			} catch(Exception erro) {
				LoggerFactory.getLogger(FiltroContextoUsuario.class).error("Falha na validação do contexto; traceId={}",MDC.get("traceId"));
				problemas.escrever(res,CodigoErro.ERRO_INTERNO,"Não foi possível concluir a operação.");return;
			}
		}
		cadeia.doFilter(req,res);
	}
	private void validar(HttpServletRequest req) throws Exception {
		var contexto=usuario.obter();
		var cadeia=rotas.getObject().getHandler(req);
		if(cadeia==null||!(cadeia.getHandler() instanceof HandlerMethod metodo))return;
		boolean rotaAdmin=metodo.hasMethodAnnotation(RotaAdmin.class)||AnnotatedElementUtils.findMergedAnnotation(metodo.getBeanType(),RotaAdmin.class)!=null;
		if(rotaAdmin&&contexto.perfil()!=Perfil.ADMIN)throw new ExcecaoNegocio(CodigoErro.PERFIL_INVALIDO,null);
		// RN-LGPD-05: o admin só usa rotas de admin, a identidade (/eu) e a autenticação; qualquer outra rota é de dado do usuário
		if(!rotaAdmin&&contexto.perfil()==Perfil.ADMIN&&!permitidaAoAdmin(req.getRequestURI()))throw new ExcecaoNegocio(CodigoErro.PERFIL_INVALIDO,null);
		var regra=metodo.getMethodAnnotation(RotaNutri.class);
		if(regra==null)regra=AnnotatedElementUtils.findMergedAnnotation(metodo.getBeanType(),RotaNutri.class);
		if(regra==null)return;
		if(regra.exclusiva()&&contexto.perfil()!=Perfil.NUTRICIONISTA)throw new ExcecaoNegocio(CodigoErro.PERFIL_INVALIDO,null);
		if(contexto.perfil()==Perfil.NUTRICIONISTA&&java.util.Set.of("POST","PUT","PATCH","DELETE").contains(req.getMethod()))guarda.validar();
	}
	private static boolean permitidaAoAdmin(String caminho) {
		return caminho.equals("/api/v1/eu")||caminho.startsWith("/api/v1/auth/")||caminho.startsWith("/legal/");
	}
}
