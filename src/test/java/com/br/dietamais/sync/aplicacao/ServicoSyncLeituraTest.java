package com.br.dietamais.sync.aplicacao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.pacientes.api.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class ServicoSyncLeituraTest {
	final UsuarioAtual usuario=mock(UsuarioAtual.class);final DiarioSync diario=mock(DiarioSync.class);
	final FonteSync<Object> pacientes=fonte(ColecaoSync.PACIENTES),vinculos=fonte(ColecaoSync.VINCULOS),nutris=fonte(ColecaoSync.NUTRIS);
	final List<FonteSync<Object>> fase2=List.of(fonte(ColecaoSync.GRUPOS),fonte(ColecaoSync.ALIMENTOS),fonte(ColecaoSync.LISTAS),fonte(ColecaoSync.DIETAS),fonte(ColecaoSync.SNAPSHOTS),fonte(ColecaoSync.DIETA_EM_USO),fonte(ColecaoSync.CHECKINS),fonte(ColecaoSync.AGUA),fonte(ColecaoSync.PESOS),fonte(ColecaoSync.AVALIACOES),fonte(ColecaoSync.MODELOS),fonte(ColecaoSync.QUESTIONARIOS),fonte(ColecaoSync.CONSULTAS),fonte(ColecaoSync.AVISOS));
	List<FonteSync<?>> todas(FonteSync<Object>... fase1){var l=new java.util.ArrayList<FonteSync<?>>(List.of(fase1));l.addAll(fase2);return l;}
	final UUID uid=UUID.randomUUID(),nid=UUID.randomUUID(),pid=UUID.randomUUID(),org=UUID.randomUUID();
	final ContextoUsuario contexto=new ContextoUsuario(new UsuarioResposta(uid,"Fictício","ficticio@example.invalid",true,List.of(Perfil.NUTRICIONISTA),Perfil.NUTRICIONISTA,nid,null,"UTC"),org);
	@SuppressWarnings("unchecked") FonteSync<Object> fonte(ColecaoSync colecao){var fonte=(FonteSync<Object>)mock(FonteSync.class);when(fonte.colecao()).thenReturn(colecao);return fonte;}
	ServicoSyncLeitura caso(){when(usuario.obter()).thenReturn(contexto);when(diario.marcoAtual()).thenReturn(100L);return new ServicoSyncLeitura(usuario,diario,todas(pacientes,vinculos,nutris));}
	@Test void paginaGlobalSoCarregaSelecionadosERevisaoNaoSubstituiVersaoClinica() {
		var caso=caso();var p=new ReferenciaSync(5,pid,nid,pid);var v=new ReferenciaSync(10,UUID.randomUUID(),nid,pid);var n=new ReferenciaSync(30,nid,nid,null);
		when(pacientes.mudancasDesde(0,contexto,3)).thenReturn(List.of(p));when(vinculos.mudancasDesde(0,contexto,3)).thenReturn(List.of(v));when(nutris.mudancasDesde(0,contexto,3)).thenReturn(List.of(n));
		var ficha=new PacienteResposta(pid,UUID.randomUUID(),"Fictício",null,null,null,null,null,null,null,StatusVinculo.CONVIDADO,99L,Instant.EPOCH,Instant.EPOCH,null);
		when(pacientes.carregar(p,contexto)).thenReturn(ficha);
		var pagina=caso.executar(null,2);
		assertThat(pagina.cursor()).isEqualTo("10");assertThat(pagina.temMais()).isTrue();
		assertThat(pagina.mudancas().pacientes().getFirst().versao()).isEqualTo(5);assertThat(pagina.mudancas().pacientes().getFirst().dados().versao()).isEqualTo(99);
		assertThat(pagina.mudancas().vinculos().getFirst().excluido()).isTrue();assertThat(pagina.mudancas().vinculos().getFirst().dados()).isNull();verify(nutris,never()).carregar(any(),any());
	}
	@Test void semMaisDadosAvancaAteMarcoDoSnapshotMesmoSemMudancaVisivel() {
		var resposta=caso().executar("20",1);assertThat(resposta.cursor()).isEqualTo("100");assertThat(resposta.temMais()).isFalse();assertThat(resposta.mudancas().pacientes()).isEmpty();
	}
	@Test void cursorFuturoEPerfilAdminNaoAcessamFontesClinicas() {
		var caso=caso();assertThatThrownBy(()->caso.executar("101",1)).isInstanceOf(ExcecaoNegocio.class);
		when(usuario.obter()).thenReturn(new ContextoUsuario(new UsuarioResposta(uid,"Admin fictício","ficticio@example.invalid",true,List.of(Perfil.ADMIN),Perfil.ADMIN,null,null,"UTC"),null));
		assertThatThrownBy(()->caso.executar(null,1)).isInstanceOfSatisfying(ExcecaoNegocio.class,erro->assertThat(erro.codigo()).isEqualTo(CodigoErro.PERFIL_INVALIDO));
		for(var fonte:List.of(pacientes,vinculos,nutris)){verify(fonte,never()).mudancasDesde(anyLong(),any(),anyInt());verify(fonte,never()).carregar(any(),any());}
	}
	@SuppressWarnings("unchecked") @Test void fontesIncompletasOuRevisoesDuplicadasNaoProduzemCursorQuePuleRegistros() {
		assertThatThrownBy(()->new ServicoSyncLeitura(usuario,diario,todas(pacientes,vinculos))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(()->new ServicoSyncLeitura(usuario,diario,todas(pacientes,vinculos,pacientes))).isInstanceOf(IllegalArgumentException.class);
		var caso=caso();when(pacientes.mudancasDesde(0,contexto,2)).thenReturn(List.of(new ReferenciaSync(5,pid,nid,pid)));when(vinculos.mudancasDesde(0,contexto,2)).thenReturn(List.of(new ReferenciaSync(5,UUID.randomUUID(),nid,pid)));
		assertThatThrownBy(()->caso.executar(null,1)).isInstanceOf(IllegalStateException.class);verify(pacientes,never()).carregar(any(),any());verify(vinculos,never()).carregar(any(),any());
	}
}
