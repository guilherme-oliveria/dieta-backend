package com.br.dietamais.alimentos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.alimentos.aplicacao.GerenciarAlimentos;
import com.br.dietamais.alimentos.infra.*;
import com.br.dietamais.comum.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class GerenciarAlimentosTest {
	final ListasAlimentosStore listas=mock(ListasAlimentosStore.class);final CatalogoAlimentosStore grupos=mock(CatalogoAlimentosStore.class);
	final UsuarioAtual usuario=mock(UsuarioAtual.class);final GuardaEscritaNutri guarda=mock(GuardaEscritaNutri.class);final GeradorIds ids=mock(GeradorIds.class);
	final UUID nid=UUID.randomUUID(),uid=UUID.randomUUID(),org=UUID.randomUUID(),grupo=UUID.randomUUID(),id=UUID.fromString("019a0000-0001-7000-8000-000000009999");
	final Instant agora=Instant.parse("2026-10-27T12:00:00Z");
	final GerenciarAlimentos caso=new GerenciarAlimentos(listas,grupos,usuario,guarda,ids,Clock.fixed(agora,ZoneOffset.UTC),JsonMapper.builder().build());
	final AtualizarAlimentoRequisicao entrada=new AtualizarAlimentoRequisicao(grupo,"Fictício","1 unidade",BigDecimal.TEN,false);
	void contexto(Perfil perfil){when(usuario.obter()).thenReturn(new ContextoUsuario(new UsuarioResposta(uid,"Fictício","ficticio@example.invalid",true,List.of(perfil),perfil,nid,null,"UTC"),org));}
	AlimentoResposta alimento(){return new AlimentoResposta(id,null,grupo,"Fictício","1 unidade",BigDecimal.TEN,true,2L,agora);}
	@Test void perfilIncorretoEAssinaturaInativaNaoEscrevem(){
		contexto(Perfil.PACIENTE);assertThatThrownBy(()->caso.atualizar(id,"\"2\"",entrada)).isInstanceOf(ExcecaoNegocio.class);verifyNoInteractions(listas,grupos,guarda);
		contexto(Perfil.NUTRICIONISTA);doThrow(new ExcecaoNegocio(CodigoErro.ASSINATURA_INATIVA,"RN-ASS-04")).when(guarda).validar();
		assertThatThrownBy(()->caso.atualizar(id,"\"2\"",entrada)).isInstanceOf(ExcecaoNegocio.class);verifyNoInteractions(listas,grupos);
	}
	@Test void invisivelRetorna404AntesDaVersaoEConflitoNaoAtualiza(){
		contexto(Perfil.NUTRICIONISTA);assertThatThrownBy(()->caso.atualizar(id,"invalido",entrada)).isInstanceOfSatisfying(ExcecaoNegocio.class,e->assertThat(e.codigo()).isEqualTo(CodigoErro.RECURSO_NAO_ENCONTRADO));
		when(listas.alimento(id,nid,true)).thenReturn(alimento());
		assertThatThrownBy(()->caso.atualizar(id,"\"1\"",entrada)).isInstanceOfSatisfying(ExcecaoNegocio.class,e->assertThat(e.codigo()).isEqualTo(CodigoErro.CONFLITO_VERSAO));
		verify(listas,never()).atualizar(any(),any(),any(),any());
	}
	@Test void grupoInvisivelNaoPermiteCriarAlimento(){
		contexto(Perfil.NUTRICIONISTA);when(ids.novo()).thenReturn(id);
		assertThatThrownBy(()->caso.criar(new CriarAlimentoRequisicao(null,grupo,"Fictício","1 unidade",BigDecimal.TEN))).isInstanceOfSatisfying(ExcecaoNegocio.class,e->assertThat(e.codigo()).isEqualTo(CodigoErro.RECURSO_NAO_ENCONTRADO));
		verify(listas,never()).criar(any(),any(),any(),any());
	}
	@Test void conflitoDoGrupoNaoAlteraBaseNemCopia(){
		contexto(Perfil.NUTRICIONISTA);when(grupos.grupo(grupo,nid,true)).thenReturn(new GrupoAlimentoResposta(grupo,"Fictício",BigDecimal.TEN,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,true,3L,OrigemMacros.CATALOGO_ESTIMADO));
		assertThatThrownBy(()->caso.atualizarMacros(grupo,"\"2\"",new AtualizarMacrosGrupoRequisicao(BigDecimal.ZERO,BigDecimal.ONE,BigDecimal.TEN))).isInstanceOfSatisfying(ExcecaoNegocio.class,e->assertThat(e.codigo()).isEqualTo(CodigoErro.CONFLITO_VERSAO));
		verify(grupos,never()).atualizar(any(),any(),any(),any(),any());
	}
	@Test void ajusteDeMacroUsaDonoUsuarioEClockDoServidor(){
		contexto(Perfil.NUTRICIONISTA);var antes=new GrupoAlimentoResposta(grupo,"Fictício",BigDecimal.TEN,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,true,0L,OrigemMacros.CATALOGO_ESTIMADO);
		var depois=new GrupoAlimentoResposta(grupo,"Fictício",BigDecimal.TEN,BigDecimal.TEN,BigDecimal.ONE,BigDecimal.ZERO,null,1L,OrigemMacros.AJUSTE_NUTRI);
		when(grupos.grupo(grupo,nid,true)).thenReturn(antes);when(grupos.grupo(grupo,nid,false)).thenReturn(depois);
		var requisicao=new AtualizarMacrosGrupoRequisicao(BigDecimal.TEN,BigDecimal.ONE,BigDecimal.ZERO);
		assertThat(caso.atualizarMacros(grupo,"\"0\"",requisicao)).isEqualTo(depois);verify(grupos).atualizar(grupo,nid,uid,requisicao,agora);
	}
	@Test void criacaoGeraIdNoServidorENaoRecebeDonoDoCliente(){
		contexto(Perfil.NUTRICIONISTA);when(ids.novo()).thenReturn(id);when(grupos.grupo(grupo,nid,false)).thenReturn(new GrupoAlimentoResposta(grupo,"Fictício",BigDecimal.TEN,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,true));
		when(listas.criar(eq(id),eq(nid),any(),eq(agora))).thenReturn(true);when(listas.alimento(id,nid,false)).thenReturn(alimento());
		assertThat(caso.criar(new CriarAlimentoRequisicao(null,grupo," Fictício "," 1 unidade ",BigDecimal.TEN)).id()).isEqualTo(id);
		verify(listas).criar(id,nid,new com.br.dietamais.alimentos.dominio.DadosAlimento(grupo,"Fictício","1 unidade",BigDecimal.TEN,true),agora);
	}
}
