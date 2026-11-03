package com.br.dietamais.comum.infra;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.api.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PublicadorAuditoriaTest {
	@Test void publicaSomenteMetadadosComAtorRealERelogioSemQueryOuHeaderForjado() {
		var uid=UUID.randomUUID();var nid=UUID.randomUUID();var org=UUID.randomUUID();
		UsuarioAtual usuario=()->new ContextoUsuario(new UsuarioResposta(uid,"Fictícia","ficticio@example.invalid",true,List.of(Perfil.NUTRICIONISTA),Perfil.NUTRICIONISTA,nid,null,"America/Sao_Paulo"),org);
		var req=new MockHttpServletRequest();req.setRemoteAddr("192.0.2.5");req.addHeader("X-Forwarded-For","198.51.100.99");req.setQueryString("token=SEGREDO_FICTICIO");
		var evento=new AtomicReference<Object>();var agora=Instant.parse("2026-10-25T12:00:00Z");
		var publicador=new PublicadorAuditoria(usuario,evento::set,Clock.fixed(agora,ZoneOffset.UTC),req);
		var referencia=new ReferenciaClinica("vinculo",UUID.randomUUID(),nid,UUID.randomUUID(),org);
		publicador.leitura(referencia);var acesso=(AcessoClinico)evento.get();
		assertThat(acesso.usuarioId()).isEqualTo(uid);assertThat(acesso.ocorridoEm()).isEqualTo(agora);
		assertThat(acesso.origemIp()).isEqualTo("192.0.2.5");assertThat(acesso.acao()).isEqualTo("LEITURA");
		assertThat(acesso.toString()).doesNotContain("SEGREDO_FICTICIO","198.51.100.99","ficticio@example.invalid");
		publicador.escrita(referencia);assertThat(((AcessoClinico)evento.get()).acao()).isEqualTo("ESCRITA");
	}
}
