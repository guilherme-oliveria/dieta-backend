package com.br.dietamais.contas.infra;

import com.br.dietamais.comum.api.*;
import java.util.UUID;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope(proxyMode=ScopedProxyMode.INTERFACES)
public class UsuarioAtualRequisicao implements UsuarioAtual {
	private final SessaoStore contas;
	private ContextoUsuario contexto;
	public UsuarioAtualRequisicao(SessaoStore contas){this.contas=contas;}
	@Override public java.util.Optional<ContextoUsuario> opcional() {
		var autenticacao=SecurityContextHolder.getContext().getAuthentication();
		if(autenticacao==null||autenticacao instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)return java.util.Optional.empty();
		return java.util.Optional.of(obter());
	}
	@Override public ContextoUsuario obter() {
		if(contexto!=null)return contexto;
		var autenticacao=SecurityContextHolder.getContext().getAuthentication();
		if(autenticacao==null||!autenticacao.isAuthenticated()||!(autenticacao.getPrincipal() instanceof Jwt jwt))throw invalido();
		UUID usuario;
		Perfil perfil;
		try { usuario=UUID.fromString(jwt.getSubject());perfil=Perfil.valueOf(jwt.getClaimAsString("perfil")); }
		catch(RuntimeException erro){throw invalido();}
		var conta=contas.consultarConta(usuario);
		if(conta==null||!conta.ativo()||!conta.perfis().contains(perfil))throw invalido();
		UUID organizacao=null;
		if(perfil==Perfil.NUTRICIONISTA) {
			if(!confere(jwt,"nid",conta.nid())||jwt.hasClaim("pid"))throw invalido();
			organizacao=contas.organizacaoNutri(conta.nid(),usuario);
			if(organizacao==null)throw invalido();
		} else if(perfil==Perfil.PACIENTE) {
			if(!confere(jwt,"pid",conta.pid())||jwt.hasClaim("nid"))throw invalido();
		} else if(jwt.hasClaim("nid")||jwt.hasClaim("pid"))throw invalido();
		contexto=new ContextoUsuario(conta.resposta(perfil),organizacao);
		return contexto;
	}
	private boolean confere(Jwt jwt,String claim,UUID identidade) {
		return identidade!=null&&identidade.toString().equals(jwt.getClaimAsString(claim));
	}
	private ExcecaoNegocio invalido(){return new ExcecaoNegocio(CodigoErro.NAO_AUTENTICADO,null);}
}
