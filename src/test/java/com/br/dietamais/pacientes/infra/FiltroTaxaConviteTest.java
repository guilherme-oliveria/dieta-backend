package com.br.dietamais.pacientes.infra;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.br.dietamais.comum.api.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;

class FiltroTaxaConviteTest {
	@Test void limitaPeloIpDaConexaoSemConfiarEmHeaderForjado() throws Exception {
		var problemas=mock(FabricaProblema.class);var cadeia=mock(jakarta.servlet.FilterChain.class);
		var filtro=new FiltroTaxaConvite(problemas,Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"),ZoneOffset.UTC),2,Duration.ofMinutes(1));
		for(int i=0;i<3;i++) {
			var pedido=new MockHttpServletRequest("GET","/api/v1/convites/AB2CD3EF");pedido.setRemoteAddr("192.0.2.5");pedido.addHeader("X-Forwarded-For","198.51.100."+i);
			var resposta=new MockHttpServletResponse();filtro.doFilter(pedido,resposta,cadeia);
			if(i==2)assertThat(resposta.getHeader("Retry-After")).isEqualTo("60");
		}
		verify(cadeia,times(2)).doFilter(any(),any());verify(problemas).escrever(any(),eq(CodigoErro.TAXA_EXCEDIDA),anyString());
	}
}
