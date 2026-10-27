package com.br.dietamais.alimentos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.alimentos.aplicacao.GerenciarListaPaciente;
import com.br.dietamais.alimentos.dominio.*;
import com.br.dietamais.alimentos.infra.ListasAlimentosStore;
import com.br.dietamais.pacientes.api.*;
import com.br.dietamais.comum.api.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class GerenciarListaPacienteTest {
	final ListasAlimentosStore store=mock(ListasAlimentosStore.class);final ConsultarFichaParaLista fichas=mock(ConsultarFichaParaLista.class);
	final UsuarioAtual usuario=mock(UsuarioAtual.class);final GuardaEscritaNutri guarda=mock(GuardaEscritaNutri.class);final AuditoriaClinica auditoria=mock(AuditoriaClinica.class);
	final UUID uid=UUID.randomUUID(),nid=UUID.randomUUID(),org=UUID.randomUUID(),pid=UUID.randomUUID(),vinculo=UUID.randomUUID(),food=UUID.randomUUID();
	final GerenciarListaPaciente caso=new GerenciarListaPaciente(store,fichas,usuario,guarda,auditoria,Clock.fixed(Instant.EPOCH,ZoneOffset.UTC),JsonMapper.builder().build());
	final AtualizarListaSubstituicaoRequisicao entrada=new AtualizarListaSubstituicaoRequisicao(List.of(food),List.of());
	void contexto(){when(usuario.obter()).thenReturn(new ContextoUsuario(new UsuarioResposta(uid,"Fictício","ficticio@example.invalid",true,List.of(Perfil.NUTRICIONISTA),Perfil.NUTRICIONISTA,nid,null,"UTC"),org));}
	@Test void historicoContinuaLegivelMasNaoEditavel(){
		contexto();var ficha=new FichaParaLista(vinculo,pid,StatusVinculo.ENCERRADO);when(fichas.consultar(pid,false)).thenReturn(ficha);when(fichas.consultar(pid,true)).thenReturn(ficha);
		when(store.ajuste(vinculo,nid,false)).thenReturn(new AjusteLista(vinculo,List.of(food),List.of(),List.of(),7));when(store.efetivos(any(),eq(nid))).thenReturn(List.of());
		assertThat(caso.consultar(pid).versao()).isEqualTo(7);verify(auditoria).leitura(any());
		assertThatThrownBy(()->caso.atualizar(pid,"\"7\"",entrada)).isInstanceOfSatisfying(ExcecaoNegocio.class,e->assertThat(e.codigo()).isEqualTo(CodigoErro.ACESSO_NEGADO));
		verify(store,never()).atualizarAjuste(any(),any(),any(),any());
	}
	@Test void alimentoForaDaListaPropriaRetorna404SemEscrever(){
		contexto();when(fichas.consultar(pid,true)).thenReturn(new FichaParaLista(vinculo,pid,StatusVinculo.ATIVO));
		when(store.ajuste(vinculo,nid,true)).thenReturn(new AjusteLista(vinculo,List.of(),List.of(),List.of(),7));
		assertThatThrownBy(()->caso.atualizar(pid,"\"7\"",entrada)).isInstanceOfSatisfying(ExcecaoNegocio.class,e->assertThat(e.codigo()).isEqualTo(CodigoErro.RECURSO_NAO_ENCONTRADO));
		verify(store,never()).atualizarAjuste(any(),any(),any(),any());verify(auditoria,never()).escrita(any());
	}
	@Test void conflitoAuditaAProjecaoPropriaSemSobrescrever(){
		contexto();when(fichas.consultar(pid,true)).thenReturn(new FichaParaLista(vinculo,pid,StatusVinculo.ATIVO));
		when(store.ajuste(vinculo,nid,true)).thenReturn(new AjusteLista(vinculo,List.of(),List.of(),List.of(),9));
		when(store.efetivos(any(),eq(nid))).thenReturn(List.of());
		assertThatThrownBy(()->caso.atualizar(pid,"\"7\"",entrada)).isInstanceOfSatisfying(ExcecaoNegocio.class,e->assertThat(e.codigo()).isEqualTo(CodigoErro.CONFLITO_VERSAO));
		verify(auditoria).leitura(any());verify(store,never()).atualizarAjuste(any(),any(),any(),any());
	}
}
