package com.br.dietamais.contas.infra;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.api.FabricaProblema;
import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import tools.jackson.databind.json.JsonMapper;

class FiltroTaxaLoginTest {
	@Test void limitaAntesDaAutenticacaoSemConfiarEmIpForjadoNoHeader() throws Exception {
		var mapper = JsonMapper.builder().build();
		var filtro = new FiltroTaxaLogin(Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC),new FabricaProblema(mapper),10);
		var processados = new AtomicInteger();
		for (int i = 0; i < 11; i++) {
			var req = new MockHttpServletRequest("POST","/api/v1/auth/login");
			req.setRemoteAddr("192.0.2.1"); req.addHeader("X-Forwarded-For","192.0.2." + (i + 2));
			var resposta = new MockHttpServletResponse();
			filtro.doFilter(req,resposta,(request,response) -> processados.incrementAndGet());
			if (i == 10) {
				assertThat(resposta.getStatus()).isEqualTo(429);
				assertThat(resposta.getHeader("Retry-After")).isEqualTo("60");
				assertThat(mapper.readTree(resposta.getContentAsString()).path("codigo").asString()).isEqualTo("TAXA_EXCEDIDA");
			}
		}
		assertThat(processados.get()).isEqualTo(10);
	}
}
