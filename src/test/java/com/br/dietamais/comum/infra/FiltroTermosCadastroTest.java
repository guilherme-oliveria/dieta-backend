package com.br.dietamais.comum.infra;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.br.dietamais.comum.api.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import tools.jackson.databind.json.JsonMapper;

class FiltroTermosCadastroTest {
	final TermosUso termos=mock(TermosUso.class);final FabricaProblema problemas=mock(FabricaProblema.class);
	@Test void bloqueiaAntesDaIdempotenciaSemAprovarOuPublicarPorDefault() throws Exception {
		doThrow(new ExcecaoNegocio(CodigoErro.TERMOS_INDISPONIVEIS,"RN-CON-09")).when(termos).validar(true,"1.0.0");
		var pedido=new MockHttpServletRequest("POST","/api/v1/auth/cadastro-nutri");pedido.setContent("{\"aceitouTermos\":true,\"versaoTermos\":\"1.0.0\"}".getBytes(StandardCharsets.UTF_8));pedido.addHeader("Idempotency-Key","chave-ficticia");
		var cadeia=mock(jakarta.servlet.FilterChain.class);new FiltroTermosCadastro(termos,problemas,JsonMapper.builder().build()).doFilter(pedido,new MockHttpServletResponse(),cadeia);
		verifyNoInteractions(cadeia);verify(problemas).escrever(any(),any(ExcecaoNegocio.class));
	}
	@Test void aceiteDeUsoDoPacienteESeparadoDoConsentimentoClinicoECorpoPreservado() throws Exception {
		String corpo="{\"aceitouTermo\":true,\"versaoTermo\":\"clinico-v1\",\"cadastro\":{\"aceitouTermos\":true,\"versaoTermos\":\"1.0.0\"}}";
		var pedido=new MockHttpServletRequest("POST","/api/v1/convites/AB2CD3EF/aceitar");pedido.setContent(corpo.getBytes(StandardCharsets.UTF_8));
		var recebido=new AtomicReference<String>();new FiltroTermosCadastro(termos,problemas,JsonMapper.builder().build()).doFilter(pedido,new MockHttpServletResponse(),(req,res)->recebido.set(new String(req.getInputStream().readAllBytes(),StandardCharsets.UTF_8)));
		verify(termos).validar(true,"1.0.0");assertThat(recebido.get()).isEqualTo(corpo);
	}
	@Test void loginEConviteSemCriacaoDeContaContinuamFuncionando() throws Exception {
		var filtro=new FiltroTermosCadastro(termos,problemas,JsonMapper.builder().build());var cadeia=mock(jakarta.servlet.FilterChain.class);
		filtro.doFilter(new MockHttpServletRequest("POST","/api/v1/auth/login"),new MockHttpServletResponse(),cadeia);
		var convite=new MockHttpServletRequest("POST","/api/v1/convites/AB2CD3EF/aceitar");convite.setContent("{\"cadastro\":null}".getBytes(StandardCharsets.UTF_8));filtro.doFilter(convite,new MockHttpServletResponse(),cadeia);
		verifyNoInteractions(termos,problemas);verify(cadeia,times(2)).doFilter(any(),any());
	}
}
