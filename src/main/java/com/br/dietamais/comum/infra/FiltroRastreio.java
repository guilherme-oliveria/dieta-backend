package com.br.dietamais.comum.infra;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FiltroRastreio extends OncePerRequestFilter {
	@Override
	protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
			FilterChain cadeia) throws ServletException, IOException {
		String recebido = requisicao.getHeader("X-Trace-Id");
		String traceId = recebido != null && recebido.matches("[0-9a-fA-F]{16,64}")
				? recebido : UUID.randomUUID().toString().replace("-", "");
		String anterior = MDC.get("traceId");
		MDC.put("traceId", traceId);
		resposta.setHeader("X-Trace-Id", traceId);
		try { cadeia.doFilter(requisicao, resposta); }
		finally {
			if (anterior == null) MDC.remove("traceId");
			else MDC.put("traceId", anterior);
		}
	}
}
