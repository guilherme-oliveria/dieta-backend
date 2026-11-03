package com.br.dietamais.contas.api;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.api.*;
import jakarta.servlet.http.Cookie;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import tools.jackson.databind.json.JsonMapper;

class TransporteSessaoTest {
	final Clock relogio = Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC);
	TransporteSessao transporte(String origem) { return new TransporteSessao(origem,relogio,new FabricaProblema(JsonMapper.builder().build())); }
	@Test void jsonNaoDependeDeCookieMasCookieExigeOriginConfigurada() {
		var req = new MockHttpServletRequest();req.setCookies(new Cookie("refreshToken","FICTICIO_SEM_VALIDADE"));
		assertThat(transporte("").lerRefresh("OUTRO_SEM_VALIDADE",req)).isEqualTo("OUTRO_SEM_VALIDADE");
		assertThatThrownBy(() -> transporte("").lerRefresh(null,req)).isInstanceOfSatisfying(ExcecaoNegocio.class,
				erro -> assertThat(erro.codigo()).isEqualTo(CodigoErro.ACESSO_NEGADO));
		req.addHeader("Origin","https://app.example.invalid");
		assertThat(transporte("https://app.example.invalid").lerRefresh(null,req)).isEqualTo("FICTICIO_SEM_VALIDADE");
	}
	@Test void pedidoDoSiteRecebeRefreshSoNoCookie() {
		var sessao = new SessaoResposta("ACESSO_FICTICIO","REFRESH_FICTICIO",relogio.instant(),null);
		var site = new MockHttpServletRequest();site.addHeader("Origin","https://app.example.invalid");
		var doSite = transporte("https://app.example.invalid").sessao(sessao,site);
		assertThat(doSite.getBody().refreshToken()).isNull();
		assertThat(doSite.getBody().accessToken()).isEqualTo("ACESSO_FICTICIO");
		assertThat(doSite.getHeaders().getFirst("Set-Cookie")).contains("refreshToken=REFRESH_FICTICIO","HttpOnly");
		var app = transporte("https://app.example.invalid").sessao(sessao,new MockHttpServletRequest());
		assertThat(app.getBody().refreshToken()).isEqualTo("REFRESH_FICTICIO");
		var alheia = new MockHttpServletRequest();alheia.addHeader("Origin","https://alheio.example.invalid");
		assertThat(transporte("https://app.example.invalid").sessao(sessao,alheia).getBody().refreshToken()).isEqualTo("REFRESH_FICTICIO");
		assertThat(transporte("").sessao(sessao,site).getBody().refreshToken()).isEqualTo("REFRESH_FICTICIO");
	}
	@Test void configuraSomenteOrigemHttpsExata() {
		assertThatThrownBy(() -> transporte("http://app.example.invalid")).isInstanceOf(IllegalStateException.class);
		assertThat(transporte("http://localhost:8081")).isNotNull(); // desenvolvimento da web
		assertThat(transporte("http://127.0.0.1:8081")).isNotNull();
		assertThatThrownBy(() -> transporte("https://app.example.invalid/caminho")).isInstanceOf(IllegalStateException.class);
	}
	@Test void bloqueioRetornaTempoDoClockENaoArmazenaCache() {
		var resposta = transporte("").bloqueado(new com.br.dietamais.contas.aplicacao.LoginBloqueado(relogio.instant().plusSeconds(900)));
		assertThat(resposta.getHeaders().getFirst("Retry-After")).isEqualTo("900");
		assertThat(resposta.getHeaders().getCacheControl()).isEqualTo("no-store");
	}
}
