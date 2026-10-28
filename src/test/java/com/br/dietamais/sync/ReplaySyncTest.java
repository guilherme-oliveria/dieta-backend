package com.br.dietamais.sync;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.sync.api.*;
import com.br.dietamais.sync.aplicacao.ServicoSyncEscrita;
import com.br.dietamais.sync.infra.MutacaoStore;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import tools.jackson.databind.json.JsonMapper;

class ReplaySyncTest {
	final UsuarioAtual usuario=mock(UsuarioAtual.class);final MutacaoStore store=mock(MutacaoStore.class);final ManipuladorMutacao manipulador=mock(ManipuladorMutacao.class);
	final UUID uid=UUID.randomUUID(),nid=UUID.randomUUID(),pid=UUID.randomUUID(),oid=UUID.randomUUID(),chave=UUID.randomUUID(),id=UUID.randomUUID();
	final JsonMapper mapper=JsonMapper.builder().build();
	final AutenticadorConteudo autenticador=autenticador();
	final ContadorTransacoes transacoes=new ContadorTransacoes();
	static class ContadorTransacoes implements PlatformTransactionManager {
		int rollbacks;
		public TransactionStatus getTransaction(TransactionDefinition d){return new SimpleTransactionStatus();}
		public void commit(TransactionStatus s){}
		public void rollback(TransactionStatus s){rollbacks++;}
	}
	ServicoSyncEscrita servico(){when(manipulador.entidade()).thenReturn("avaliacoes");return new ServicoSyncEscrita(usuario,store,List.of(manipulador),transacoes,autenticador,mapper);}
	AutenticadorConteudo autenticador(){byte[] chave=new byte[32];new java.security.SecureRandom().nextBytes(chave);return new com.br.dietamais.comum.infra.CifradorResposta(Base64.getEncoder().encodeToString(chave));}
	String fingerprint(String peso){var m=mutacao(peso);return autenticador.autenticarRequisicao(com.br.dietamais.sync.dominio.ConteudoMutacao.canonico(new MutacaoSync(m.idMutacao(),m.entidade(),m.operacao(),m.id(),m.versaoBase(),m.dados()),mapper)).orElseThrow();}
	void perfil(Perfil perfil){when(usuario.obter()).thenReturn(new ContextoUsuario(new UsuarioResposta(uid,"Fictício","ficticio@example.invalid",true,List.of(Perfil.NUTRICIONISTA,Perfil.PACIENTE),perfil,nid,pid,"UTC"),oid));}
	MutacaoSyncRequisicao mutacao(String peso){return new MutacaoSyncRequisicao(chave,"avaliacoes",OperacaoSync.ATUALIZAR,id,1L,mapper.readTree("{\"pesoKg\":"+peso+"}"));}
	ResultadoMutacao enviar(ServicoSyncEscrita servico,String peso){return servico.enviar(new EnviarMutacoesRequisicao(List.of(mutacao(peso)))).resultados().getFirst();}
	@Test void mesmoUsuarioEmOutroPerfilNaoRecebeConflitoClinicoGuardado(){
		perfil(Perfil.PACIENTE);var clinico=new ResultadoMutacao(chave,ResultadoMutacao.Status.CONFLITO,null,"CONFLITO_VERSAO","RN-SYN-02",mapper.readTree("{\"pesoKg\":70}"));when(store.buscar(chave)).thenReturn(new MutacaoStore.Guardada(uid,Perfil.NUTRICIONISTA,fingerprint("70"),clinico));
		var resultado=enviar(servico(),"70");assertThat(resultado.status()).isEqualTo(ResultadoMutacao.Status.REJEITADA);assertThat(resultado.codigo()).isEqualTo("CHAVE_IDEMPOTENCIA_REUTILIZADA");assertThat(resultado.atual()).isNull();verify(manipulador,never()).aplicar(any(),any());
	}
	@Test void mesmoIdComCorpoAlteradoNaoReutilizaResultadoAnterior(){
		perfil(Perfil.NUTRICIONISTA);when(store.buscar(chave)).thenReturn(new MutacaoStore.Guardada(uid,Perfil.NUTRICIONISTA,fingerprint("70"),ResultadoMutacao.aplicada(chave,2)));
		var resultado=enviar(servico(),"71");assertThat(resultado.status()).isEqualTo(ResultadoMutacao.Status.REJEITADA);assertThat(resultado.codigo()).isEqualTo("CHAVE_IDEMPOTENCIA_REUTILIZADA");verify(manipulador,never()).aplicar(any(),any());
	}
	@Test void replayLegitimoPreservaInclusiveConflitoSemChamarManipulador(){
		perfil(Perfil.NUTRICIONISTA);var resultado=new ResultadoMutacao(chave,ResultadoMutacao.Status.CONFLITO,null,"CONFLITO_VERSAO","RN-SYN-02",mapper.readTree("{\"pesoKg\":70}"));when(store.buscar(chave)).thenReturn(new MutacaoStore.Guardada(uid,Perfil.NUTRICIONISTA,fingerprint("70"),resultado));
		assertThat(enviar(servico(),"70.0")).isEqualTo(resultado);verify(manipulador,never()).aplicar(any(),any());
	}
	@Test void legadoSemMetadadosEDonoDiferenteNaoExponhamResultadoNemReexecutam(){
		perfil(Perfil.NUTRICIONISTA);var resultado=ResultadoMutacao.aplicada(chave,2);when(store.buscar(chave)).thenReturn(new MutacaoStore.Guardada(uid,resultado));var caso=servico();
		assertThat(enviar(caso,"70").codigo()).isEqualTo("CHAVE_IDEMPOTENCIA_REUTILIZADA");
		when(store.buscar(chave)).thenReturn(new MutacaoStore.Guardada(UUID.randomUUID(),Perfil.NUTRICIONISTA,fingerprint("70"),resultado));assertThat(enviar(caso,"70").codigo()).isEqualTo("CHAVE_IDEMPOTENCIA_REUTILIZADA");verify(manipulador,never()).aplicar(any(),any());verify(store,never()).guardar(any(),any(),any(),any());
	}
	@Test void criacaoEReplayUsamMesmaTransacaoComBloqueioEMetadados(){
		perfil(Perfil.NUTRICIONISTA);var registro=new java.util.concurrent.atomic.AtomicReference<MutacaoStore.Guardada>();when(store.buscar(chave)).thenAnswer(a->registro.get());
		when(store.guardar(eq(uid),eq(Perfil.NUTRICIONISTA),anyString(),any())).thenAnswer(a->{registro.set(new MutacaoStore.Guardada(a.getArgument(0),a.getArgument(1),a.getArgument(2),a.getArgument(3)));return true;});
		when(manipulador.aplicar(any(),any())).thenReturn(ResultadoMutacao.aplicada(chave,2));var caso=servico();assertThat(enviar(caso,"70")).isEqualTo(enviar(caso,"70.0"));verify(manipulador,times(1)).aplicar(any(),any());
		var ordem=inOrder(store,manipulador);ordem.verify(store).bloquear(chave);ordem.verify(store).buscar(chave);ordem.verify(manipulador).aplicar(any(),any());ordem.verify(store).guardar(eq(uid),eq(Perfil.NUTRICIONISTA),eq(fingerprint("70")),any());
	}
	@Test void reservaNaoGravadaNaoPodeRetornarAplicada(){
		perfil(Perfil.NUTRICIONISTA);when(manipulador.aplicar(any(),any())).thenReturn(ResultadoMutacao.aplicada(chave,2));assertThatThrownBy(()->enviar(servico(),"70")).isInstanceOf(IllegalStateException.class);assertThat(transacoes.rollbacks).isEqualTo(1);
	}
	@Test void rejeicaoConfirmadaTambemTemReplayDeterministicoSemReexecutar(){
		perfil(Perfil.NUTRICIONISTA);var registro=new java.util.concurrent.atomic.AtomicReference<MutacaoStore.Guardada>();when(store.buscar(chave)).thenAnswer(a->registro.get());
		when(store.guardar(eq(uid),eq(Perfil.NUTRICIONISTA),anyString(),any())).thenAnswer(a->{registro.set(new MutacaoStore.Guardada(a.getArgument(0),a.getArgument(1),a.getArgument(2),a.getArgument(3)));return true;});when(manipulador.aplicar(any(),any())).thenThrow(new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null));
		var caso=servico();var resultado=enviar(caso,"70");assertThat(resultado.codigo()).isEqualTo("RECURSO_NAO_ENCONTRADO");assertThat(enviar(caso,"70.0")).isEqualTo(resultado);verify(manipulador,times(1)).aplicar(any(),any());assertThat(transacoes.rollbacks).isEqualTo(1);
	}
	@Test void rejeicaoReconsultaDepoisDoRollbackSemSobrescreverVencedor(){
		perfil(Perfil.NUTRICIONISTA);when(store.buscar(chave)).thenReturn(null,new MutacaoStore.Guardada(uid,Perfil.PACIENTE,fingerprint("70"),ResultadoMutacao.aplicada(chave,9)));
		when(manipulador.aplicar(any(),any())).thenThrow(new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null));assertThat(enviar(servico(),"70").codigo()).isEqualTo("CHAVE_IDEMPOTENCIA_REUTILIZADA");verify(store,times(2)).bloquear(chave);verify(store,never()).guardar(any(),any(),any(),any());
	}
	@Test void perfilNutriTambemNaoRecebeResultadoDoPerfilPaciente(){
		perfil(Perfil.NUTRICIONISTA);when(store.buscar(chave)).thenReturn(new MutacaoStore.Guardada(uid,Perfil.PACIENTE,fingerprint("70"),ResultadoMutacao.aplicada(chave,2)));assertThat(enviar(servico(),"70").codigo()).isEqualTo("CHAVE_IDEMPOTENCIA_REUTILIZADA");verify(manipulador,never()).aplicar(any(),any());
	}
	@Test void chaveAusenteNaoPermiteArmazenarFingerprintSemAutenticacao(){
		perfil(Perfil.NUTRICIONISTA);when(manipulador.entidade()).thenReturn("avaliacoes");var caso=new ServicoSyncEscrita(usuario,store,List.of(manipulador),transacoes,dados->Optional.empty(),mapper);assertThatThrownBy(()->enviar(caso,"70")).isInstanceOf(IllegalStateException.class);verifyNoInteractions(store);
	}
	@Test void exemploOficialDeReusoPreservaContratoSemConteudoAnterior()throws Exception{
		try(var recurso=getClass().getResourceAsStream("/contrato/sync-replay/rejeicao-reuso.json")){
			assertThat(recurso).isNotNull();var json=mapper.readTree(recurso.readAllBytes());var resposta=mapper.treeToValue(json,EnviarMutacoesResposta.class);assertThat(mapper.readTree(mapper.writeValueAsString(resposta))).isEqualTo(json);assertThat(resposta.resultados().getFirst().atual()).isNull();assertThat(resposta.resultados().getFirst().versao()).isNull();
		}
	}
}
