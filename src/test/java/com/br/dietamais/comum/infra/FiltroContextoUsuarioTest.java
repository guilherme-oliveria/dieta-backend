package com.br.dietamais.comum.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import com.br.dietamais.comum.api.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

class FiltroContextoUsuarioTest {
	final UsuarioAtual usuario=mock(UsuarioAtual.class);
	final GuardaEscritaNutri guarda=mock(GuardaEscritaNutri.class);
	final FabricaProblema problemas=mock(FabricaProblema.class);
	final RequestMappingHandlerMapping rotas=mock(RequestMappingHandlerMapping.class);
	@SuppressWarnings("unchecked") final ObjectProvider<RequestMappingHandlerMapping> provider=mock(ObjectProvider.class);
	final MockHttpServletRequest pedido=new MockHttpServletRequest("POST","/clinico");
	final MockHttpServletResponse resposta=new MockHttpServletResponse();
	final jakarta.servlet.FilterChain cadeia=mock(jakarta.servlet.FilterChain.class);
	@BeforeEach void configurar() throws Exception {
		when(provider.getObject()).thenReturn(rotas);
		when(rotas.getHandler(pedido)).thenReturn(new HandlerExecutionChain(new HandlerMethod(new RotaTeste(),RotaTeste.class.getDeclaredMethod("escrever"))));
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("ficticio","",List.of()));
	}
	@AfterEach void limpar(){SecurityContextHolder.clearContext();}
	ContextoUsuario contexto(Perfil perfil) {
		return new ContextoUsuario(new UsuarioResposta(UUID.randomUUID(),"Fictício","ficticio@example.invalid",false,List.of(perfil),perfil,
			perfil==Perfil.NUTRICIONISTA?UUID.randomUUID():null,perfil==Perfil.PACIENTE?UUID.randomUUID():null,"UTC"),UUID.randomUUID());
	}
	void executar() throws Exception {new FiltroContextoUsuario(usuario,guarda,problemas,provider).doFilter(pedido,resposta,cadeia);}
	@Test void escritaConfereContextoEGuardaAntesDaCadeia() throws Exception {
		when(usuario.obter()).thenReturn(contexto(Perfil.NUTRICIONISTA));executar();
		var ordem=inOrder(usuario,guarda,cadeia);ordem.verify(usuario).obter();ordem.verify(guarda).validar();ordem.verify(cadeia).doFilter(pedido,resposta);
	}
	@Test void leituraNaoDependeDoPlano() throws Exception {
		pedido.setMethod("GET");when(usuario.obter()).thenReturn(contexto(Perfil.NUTRICIONISTA));executar();
		verifyNoInteractions(guarda);verify(cadeia).doFilter(pedido,resposta);
	}
	@Test void perfilPacienteNaoAcessaRotaNutri() throws Exception {
		when(usuario.obter()).thenReturn(contexto(Perfil.PACIENTE));executar();
		verify(problemas).escrever(eq(resposta),argThat((ExcecaoNegocio erro)->erro.codigo()==CodigoErro.PERFIL_INVALIDO));
		verifyNoInteractions(cadeia,guarda);
	}
	@Test void revogacaoNoBancoInterrompeMesmoRepeticaoIdempotente() throws Exception {
		pedido.addHeader("Idempotency-Key",UUID.randomUUID().toString());
		when(usuario.obter()).thenThrow(new ExcecaoNegocio(CodigoErro.NAO_AUTENTICADO,null));executar();
		assertThat(resposta.getHeader("WWW-Authenticate")).isEqualTo("Bearer");verifyNoInteractions(cadeia,guarda);
	}
	@Test void planoBloqueadoInterrompeAntesDaCadeia() throws Exception {
		when(usuario.obter()).thenReturn(contexto(Perfil.NUTRICIONISTA));
		doThrow(new ExcecaoNegocio(CodigoErro.ASSINATURA_INATIVA,"RN-ASS-04")).when(guarda).validar();executar();
		verifyNoInteractions(cadeia);
		verify(problemas).escrever(eq(resposta),argThat((ExcecaoNegocio erro)->erro.codigo()==CodigoErro.ASSINATURA_INATIVA));
	}
	@RotaNutri static class RotaTeste { public void escrever(){} }
}
