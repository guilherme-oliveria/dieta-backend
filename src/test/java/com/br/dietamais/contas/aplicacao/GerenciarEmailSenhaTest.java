package com.br.dietamais.contas.aplicacao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.dominio.PoliticaSenha;
import com.br.dietamais.contas.infra.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class GerenciarEmailSenhaTest {
	@Test void falhaDeEnfileiramentoFazRollbackSemDistinguirUsuarioNaResposta() {
		var contas=mock(SessaoStore.class);var tokens=mock(TokenContaStore.class);var emissor=mock(EmitirTokenConta.class);
		var transacoes=mock(PlatformTransactionManager.class);when(transacoes.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
		var id=UUID.randomUUID();when(contas.bloquearContaPorEmail(anyString())).thenReturn(id);
		when(contas.consultarConta(id)).thenReturn(new SessaoStore.Conta(id,"Teste","ficticio@example.invalid","HASH_FICTICIO",true,false,Perfil.NUTRICIONISTA,UUID.randomUUID(),null,"America/Sao_Paulo"));
		doThrow(new IllegalStateException("Falha fictícia da fila")).when(emissor).emitir(any(),anyString(),any());
		var servico=new GerenciarEmailSenha(contas,tokens,emissor,new PoliticaSenha(Set.of()),mock(PasswordEncoder.class),Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"),ZoneOffset.UTC),transacoes);
		assertThatCode(()->servico.solicitarRecuperacao("ficticio@example.invalid")).doesNotThrowAnyException();
		verify(transacoes).rollback(any());verify(transacoes,never()).commit(any());
	}
}
