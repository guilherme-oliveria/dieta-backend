package com.br.dietamais.contas.aplicacao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.br.dietamais.contas.api.*;
import com.br.dietamais.contas.infra.PerfilNutriStore;
import com.br.dietamais.comum.api.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class GerenciarPerfilNutriTest {
	final UsuarioAtual usuario=mock(UsuarioAtual.class);
	final GuardaEscritaNutri guarda=mock(GuardaEscritaNutri.class);
	final PerfilNutriStore store=mock(PerfilNutriStore.class);
	final UUID uid=UUID.randomUUID(),nid=UUID.randomUUID(),org=UUID.randomUUID();
	final Instant agora=Instant.parse("2026-10-27T02:00:00Z");
	final GerenciarPerfilNutri caso=new GerenciarPerfilNutri(usuario,guarda,store,Clock.fixed(agora,ZoneOffset.UTC),JsonMapper.builder().build());
	final AtualizarPerfilNutriRequisicao requisicao=new AtualizarPerfilNutriRequisicao(" Nutri fictícia "," CRN-1 00000 ","  "," Texto fictício ");
	final PerfilNutriResposta atual=new PerfilNutriResposta(nid,"Nome fictício","CRN-1 00000",null,null,false,4L,agora);
	void contexto(Perfil perfil){when(usuario.obter()).thenReturn(new ContextoUsuario(new UsuarioResposta(uid,"Fictício","ficticio@example.invalid",true,List.of(perfil),perfil,nid,UUID.randomUUID(),"UTC"),org));}
	@Test void leituraPropriaDispensaAssinaturaEEscrita() {
		contexto(Perfil.NUTRICIONISTA);when(store.consultar(nid,uid,org,false)).thenReturn(atual);
		assertThat(caso.consultar()).isEqualTo(atual);verifyNoInteractions(guarda);verify(store,never()).atualizar(any(),any(),any(),any());
	}
	@Test void pacienteEAdminNaoConsultamNemAlteramPerfilDaNutri() {
		for(var perfil:List.of(Perfil.PACIENTE,Perfil.ADMIN)){contexto(perfil);assertThatThrownBy(caso::consultar).isInstanceOf(ExcecaoNegocio.class);assertThatThrownBy(()->caso.atualizar("\"4\"",requisicao)).isInstanceOf(ExcecaoNegocio.class);}
		verifyNoInteractions(store,guarda);
	}
	@Test void assinaturaInativaImpedeEscritaAntesDoCarregamento() {
		contexto(Perfil.NUTRICIONISTA);doThrow(new ExcecaoNegocio(CodigoErro.ASSINATURA_INATIVA,"RN-ASS-04")).when(guarda).validar();
		assertThatThrownBy(()->caso.atualizar("\"4\"",requisicao)).isInstanceOf(ExcecaoNegocio.class);verifyNoInteractions(store);
	}
	@Test void conflitoSoExpoeOProprioPerfilENaoEscreve() {
		contexto(Perfil.NUTRICIONISTA);when(store.consultar(nid,uid,org,true)).thenReturn(atual);
		assertThatThrownBy(()->caso.atualizar("\"3\"",requisicao)).isInstanceOfSatisfying(ExcecaoNegocio.class,e->assertThat(e.codigo()).isEqualTo(CodigoErro.CONFLITO_VERSAO));
		verify(store,never()).atualizar(any(),any(),any(),any());
	}
	@Test void identidadeDivergenteRetorna404AntesDeCompararVersao() {
		contexto(Perfil.NUTRICIONISTA);assertThatThrownBy(()->caso.atualizar("invalido",requisicao)).isInstanceOfSatisfying(ExcecaoNegocio.class,e->assertThat(e.codigo()).isEqualTo(CodigoErro.RECURSO_NAO_ENCONTRADO));
		verify(store,never()).atualizar(any(),any(),any(),any());
	}
	@Test void escritaNormalizadaUsaClockEIdentidadeDoContexto(){
		contexto(Perfil.NUTRICIONISTA);when(store.consultar(nid,uid,org,true)).thenReturn(atual);when(store.consultar(nid,uid,org,false)).thenReturn(atual);
		caso.atualizar("\"4\"",requisicao);
		verify(store).atualizar(nid,uid,new com.br.dietamais.contas.dominio.DadosPerfilNutri("Nutri fictícia","CRN-1 00000",null,"Texto fictício"),agora);
	}
}
