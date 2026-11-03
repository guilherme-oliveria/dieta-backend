package com.br.dietamais.contas.aplicacao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.api.LoginRequisicao;
import com.br.dietamais.contas.dominio.BloqueioLogin;
import com.br.dietamais.contas.infra.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class AutenticarSessaoTest {
	final Instant agora = Instant.parse("2026-10-25T12:00:00Z");
	final Clock relogio = Clock.fixed(agora, ZoneOffset.UTC);
	final SessaoStore store = mock(SessaoStore.class);
	final PlatformTransactionManager transacoes = mock(PlatformTransactionManager.class);
	final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16,32,1,19456,2);
	final ChavesJwt chaves = new ChavesJwt("","",true);
	final UUID usuario = UUID.randomUUID();
	final UUID nid = UUID.randomUUID();
	final String senha = "Frase fictícia com muitas luas";
	AutenticarSessao servico() {
		when(transacoes.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
		when(store.bloquearTentativas(anyString(),eq(agora))).thenReturn(new BloqueioLogin(List.of(),null));
		GeradorIds ids = UUID::randomUUID;
		return new AutenticarSessao(store,encoder,new TokensJwt(chaves.encoder(),relogio),ids,relogio,transacoes);
	}
	LoginRequisicao entrada(String senha) {
		return new LoginRequisicao("ficticio@example.invalid",senha,new DispositivoRequisicao(UUID.randomUUID(),Plataforma.IOS,"Teste"),null);
	}
	@Test void credenciaisValidasEmitemSessaoAssinadaSemConfirmacaoObrigatoriaDeEmail() {
		var servico = servico();
		when(store.bloquearContaPorEmail(anyString())).thenReturn(usuario);
		when(store.consultarConta(usuario)).thenReturn(new SessaoStore.Conta(usuario,"Nutri fictícia","ficticio@example.invalid",
				encoder.encode(senha),true,false,Perfil.NUTRICIONISTA,nid,null,"America/Sao_Paulo"));
		var sessao = servico.entrar(entrada(senha));
		assertThat(chaves.decoder(relogio).decode(sessao.accessToken()).getSubject()).isEqualTo(usuario.toString());
		assertThat(sessao.usuario().emailConfirmado()).isFalse();
		assertThat(sessao.refreshToken()).matches("[A-Za-z0-9_-]{43}");
		assertThat(sessao.expiraEm()).isEqualTo(agora.plusSeconds(900));
	}
	@Test void bloqueioDaQuintaFalhaPersisteAntesDaExcecaoHttp() {
		var servico = servico();
		when(store.bloquearTentativas(anyString(),eq(agora))).thenReturn(new BloqueioLogin(List.of(
				agora.minusSeconds(4),agora.minusSeconds(3),agora.minusSeconds(2),agora.minusSeconds(1)),null));
		assertThatThrownBy(() -> servico.entrar(entrada("Credencial fictícia incorreta"))).isInstanceOfSatisfying(LoginBloqueado.class,
				erro -> assertThat(erro.liberarEm()).isEqualTo(agora.plusSeconds(900)));
		var ordem = inOrder(store,transacoes);
		ordem.verify(store).salvarTentativas(anyString(),argThat(estado -> estado.bloqueado(agora)),eq(agora));
		ordem.verify(transacoes).commit(any());
		verify(transacoes,never()).rollback(any());
	}
	@Test void reusoConfirmaRevogacaoMesmoComResposta401() {
		var servico = servico(); var familia = UUID.randomUUID();
		when(store.consultarRefresh(anyString())).thenReturn(new SessaoStore.Refresh(UUID.randomUUID(),usuario,familia,
				UUID.randomUUID(),Plataforma.ANDROID,"Teste",Perfil.NUTRICIONISTA,agora.plusSeconds(3600),true,false));
		when(store.bloquearConta(usuario)).thenReturn(true);
		assertThatThrownBy(() -> servico.renovar("TOKEN_FICTICIO_SEM_VALIDADE")).isInstanceOfSatisfying(ExcecaoNegocio.class,
				erro -> assertThat(erro.codigo()).isEqualTo(CodigoErro.TOKEN_INVALIDO));
		var ordem = inOrder(store,transacoes);
		ordem.verify(store).revogarFamilia(familia,agora);
		ordem.verify(transacoes).commit(any());
		verify(transacoes,never()).rollback(any());
	}
}
