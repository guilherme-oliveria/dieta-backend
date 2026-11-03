package com.br.dietamais.contas.infra;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.br.dietamais.comum.api.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class UsuarioAtualRequisicaoTest {
	final SessaoStore store=mock(SessaoStore.class);
	final UUID usuario=UUID.randomUUID(),nid=UUID.randomUUID(),org=UUID.randomUUID();
	@AfterEach void limpar(){SecurityContextHolder.clearContext();}
	SessaoStore.Conta conta(boolean ativa){return new SessaoStore.Conta(usuario,"Fictícia","ficticio@example.invalid","HASH_FICTICIO",ativa,false,Perfil.NUTRICIONISTA,nid,null,"America/Sao_Paulo");}
	void autenticar(UUID identidade) {
		var jwt=Jwt.withTokenValue("TOKEN_FICTICIO_SEM_VALIDADE").header("alg","RS256").subject(usuario.toString())
				.claim("perfil","NUTRICIONISTA").claim("nid",identidade.toString()).build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,java.util.List.of()));
	}
	@Test void contextoUsaContaEOrganizacaoDoBanco() {
		autenticar(nid);when(store.consultarConta(usuario)).thenReturn(conta(true));when(store.organizacaoNutri(nid,usuario)).thenReturn(org);
		var contexto=new UsuarioAtualRequisicao(store).obter();
		assertThat(contexto.usuarioId()).isEqualTo(usuario);assertThat(contexto.exigirNutricionista()).isEqualTo(nid);assertThat(contexto.organizacaoId()).isEqualTo(org);
		assertThatThrownBy(contexto::exigirPaciente).isInstanceOf(ExcecaoNegocio.class);
	}
	@Test void identidadeAlheiaEContaInativaInvalidamMesmoJwtAssinado() {
		autenticar(UUID.randomUUID());when(store.consultarConta(usuario)).thenReturn(conta(true));
		assertThatThrownBy(()->new UsuarioAtualRequisicao(store).obter()).isInstanceOfSatisfying(ExcecaoNegocio.class,erro->assertThat(erro.codigo()).isEqualTo(CodigoErro.NAO_AUTENTICADO));
		autenticar(nid);when(store.consultarConta(usuario)).thenReturn(conta(false));
		assertThatThrownBy(()->new UsuarioAtualRequisicao(store).obter()).isInstanceOf(ExcecaoNegocio.class);
	}
}
