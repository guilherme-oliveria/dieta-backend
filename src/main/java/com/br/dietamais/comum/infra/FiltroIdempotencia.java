package com.br.dietamais.comum.infra;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.br.dietamais.comum.api.CodigoErro;
import com.br.dietamais.comum.api.ExcecaoNegocio;
import com.br.dietamais.comum.api.FabricaProblema;
import com.br.dietamais.comum.api.Idempotente;
import com.br.dietamais.comum.aplicacao.RespostaIdempotente;
import com.br.dietamais.comum.aplicacao.ServicoIdempotencia;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(-90) // Depois do Spring Security (-100), enquanto o contexto autenticado está ativo.
public class FiltroIdempotencia extends OncePerRequestFilter {
	private final ServicoIdempotencia servico;
	private final FabricaProblema problemas;
	private final RequestMappingHandlerMapping rotas;
	private final int limiteCorpo;
	private final CifradorResposta cifrador;

	public FiltroIdempotencia(ServicoIdempotencia servico, FabricaProblema problemas,
			@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping rotas,
			@Value("${dieta.idempotencia.limite-corpo:1048576}") int limiteCorpo, CifradorResposta cifrador) {
		if (limiteCorpo < 1) throw new IllegalArgumentException("Limite do corpo deve ser positivo");
		this.servico = servico;
		this.problemas = problemas;
		this.rotas = rotas;
		this.limiteCorpo = limiteCorpo;
		this.cifrador = cifrador;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
			throws ServletException, IOException {
		String chave = requisicao.getHeader("Idempotency-Key");
		if (!"POST".equals(requisicao.getMethod()) || chave == null || !idempotente(requisicao)) {
			cadeia.doFilter(requisicao, resposta);
			return;
		}
		try {
			if (!chave.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
				problemas.escrever(resposta, CodigoErro.FORMATO_INVALIDO, "Idempotency-Key deve ser UUID.");
				return;
			}
			byte[] corpo = requisicao.getInputStream().readNBytes(limiteCorpo + 1);
			if (corpo.length > limiteCorpo) {
				problemas.escrever(resposta, CodigoErro.FORMATO_INVALIDO, "Requisição excede o limite permitido.");
				return;
			}
			String escopo = escopo();
			UUID identificador = UUID.fromString(chave);
			var resultado = servico.executar(escopo, identificador, hash(requisicao, corpo, escopo, identificador),
					() -> executar(cadeia, new RequisicaoRepetivel(requisicao, corpo), resposta));
			resposta.setStatus(resultado.status());
			resultado.cabecalhos().forEach((nome, valores) -> {
				resposta.setHeader(nome, valores.getFirst());
				valores.stream().skip(1).forEach(valor -> resposta.addHeader(nome, valor));
			});
			resposta.getOutputStream().write(resultado.corpo());
		} catch (ExcecaoNegocio erro) {
			problemas.escrever(resposta, erro.codigo(), erro.getMessage());
		} catch (UncheckedIOException erro) { throw erro.getCause(); }
		catch (RuntimeException erro) {
			resposta.resetBuffer();
			resposta.setHeader("Location", null);
			resposta.setHeader("ETag", null);
			org.slf4j.LoggerFactory.getLogger(FiltroIdempotencia.class).error(
					"Falha na operação idempotente; traceId={}", org.slf4j.MDC.get("traceId"));
			problemas.escrever(resposta, CodigoErro.ERRO_INTERNO, "Não foi possível concluir a operação.");
		}
	}

	private boolean idempotente(HttpServletRequest requisicao) throws ServletException {
		try {
			var handler = rotas.getHandler(requisicao);
			return handler != null && handler.getHandler() instanceof HandlerMethod metodo
					&& metodo.hasMethodAnnotation(Idempotente.class);
		} catch (Exception erro) { return false; } // O MVC traduz a falha de roteamento no fluxo normal.
	}

	private RespostaIdempotente executar(FilterChain cadeia, HttpServletRequest requisicao, HttpServletResponse resposta) {
		var cache = new ContentCachingResponseWrapper(resposta);
		try { cadeia.doFilter(requisicao, cache); }
		catch (IOException erro) { throw new UncheckedIOException(erro); }
		catch (ServletException erro) { throw new IllegalStateException("Falha na operação idempotente", erro); }
		Map<String, List<String>> cabecalhos = List.of("Content-Type", "Location", "ETag", "Cache-Control").stream()
				.filter(nome -> !cache.getHeaders(nome).isEmpty())
				.collect(Collectors.toMap(nome -> nome, nome -> List.copyOf(cache.getHeaders(nome))));
		return new RespostaIdempotente(cache.getStatus(), cabecalhos, cache.getContentAsByteArray());
	}

	private String escopo() {
		var usuario = SecurityContextHolder.getContext().getAuthentication();
		if (usuario == null || usuario instanceof AnonymousAuthenticationToken || !usuario.isAuthenticated()) return "publico";
		String perfis = usuario.getAuthorities().stream().map(Object::toString).sorted().collect(Collectors.joining(","));
		return digest((usuario.getName() + "\n" + perfis).getBytes(StandardCharsets.UTF_8));
	}

	private String hash(HttpServletRequest requisicao, byte[] corpo, String escopo, UUID chave) {
		String metadados = escopo + "\n" + chave + "\n" + requisicao.getMethod() + "\n" + requisicao.getRequestURI() + "\n"
				+ requisicao.getQueryString() + "\n" + requisicao.getContentType() + "\n" + digest(corpo);
		byte[] dados = metadados.getBytes(StandardCharsets.UTF_8);
		// Sem chave, só erros/stubs podem concluir: uma resposta 2xx falha na cifra e toda a transação é desfeita.
		return cifrador.autenticarRequisicao(dados).orElseGet(() -> digest(dados));
	}

	private String digest(byte[] dados) {
		try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(dados)); }
		catch (NoSuchAlgorithmException erro) { throw new IllegalStateException("SHA-256 indisponível", erro); }
	}
}
