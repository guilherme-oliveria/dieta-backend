package com.br.dietamais.contas.api;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.aplicacao.LoginBloqueado;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

@Component
public class TransporteSessao {
	private final String origem;
	private final Clock relogio;
	private final FabricaProblema problemas;
	public TransporteSessao(@Value("${WEB_ORIGEM:}") String origem, Clock relogio, FabricaProblema problemas) {
		this.origem = origem;
		this.relogio = relogio;
		this.problemas = problemas;
		if (!origem.isBlank()) {
			var uri = URI.create(origem);
			if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getRawUserInfo() != null
					|| uri.getRawQuery() != null || uri.getRawFragment() != null || !uri.getPath().isEmpty())
				throw new IllegalStateException("WEB_ORIGEM precisa ser uma origem HTTPS sem caminho.");
		}
	}
	public String lerRefresh(String corpo, HttpServletRequest requisicao) {
		if (corpo != null && !corpo.isBlank()) return corpo;
		var cookies = requisicao.getCookies();
		if (cookies != null) for (var cookie : cookies) if ("refreshToken".equals(cookie.getName())) {
			if (origem.isBlank() || !origem.equals(requisicao.getHeader("Origin"))) throw new ExcecaoNegocio(CodigoErro.ACESSO_NEGADO, null);
			String valor = cookie.getValue();
			if (valor != null && !valor.isBlank() && valor.length() <= 4096) return valor;
		}
		throw new ExcecaoNegocio(CodigoErro.OBRIGATORIO, null);
	}
	public ResponseEntity<SessaoResposta> sessao(SessaoResposta sessao) {
		var resposta = ResponseEntity.ok().cacheControl(CacheControl.noStore());
		if (sessao.refreshToken() != null) resposta.header(HttpHeaders.SET_COOKIE, cookie(sessao.refreshToken(), Duration.ofDays(30)));
		return resposta.body(sessao);
	}
	public ResponseEntity<Void> encerrada() {
		return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
				.header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO)).build();
	}
	public ResponseEntity<ProblemDetail> bloqueado(LoginBloqueado erro) {
		long espera = Math.max(1, (Duration.between(relogio.instant(), erro.liberarEm()).toMillis() + 999) / 1000);
		return ResponseEntity.status(429).cacheControl(CacheControl.noStore()).header(HttpHeaders.RETRY_AFTER, Long.toString(espera))
				.body(problemas.criar(erro.codigo(), erro.regra(), erro.getMessage(), List.of(), null));
	}
	private String cookie(String valor, Duration prazo) {
		return ResponseCookie.from("refreshToken", valor).httpOnly(true).secure(true).sameSite("Strict")
				.path("/api/v1/auth").maxAge(prazo).build().toString();
	}
}
