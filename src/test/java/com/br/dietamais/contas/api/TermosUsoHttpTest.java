package com.br.dietamais.contas.api;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.comum.infra.*;
import com.br.dietamais.config.SecurityConfig;
import com.br.dietamais.contas.aplicacao.*;
import com.br.dietamais.contas.infra.FonteTermosUsoMarkdown;
import java.net.*;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes=TermosUsoHttpTest.AplicacaoLegal.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
	"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration","spring.docker.compose.enabled=false","springdoc.swagger-ui.enabled=false"})
class TermosUsoHttpTest {
	@Configuration(proxyBeanMethods=false) @EnableAutoConfiguration
	@Import({SecurityConfig.class,TermosUsoController.class,ServicoTermosUso.class,FonteTermosUsoMarkdown.class,FiltroTermosCadastro.class,
		FabricaProblema.class,RelogioConfig.class,AuthController.class,TransporteSessao.class})
	static class AplicacaoLegal{}
	@MockitoBean CadastrarNutri cadastro;
	@MockitoBean AutenticarSessao sessoes;
	@MockitoBean GerenciarEmailSenha emails;
	@MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder decoder;
	@LocalServerPort int porta;
	@Autowired JsonMapper mapper;
	final HttpClient cliente=HttpClient.newHttpClient();
	@AfterEach void fechar(){cliente.close();}
	@Test void rotaPublicaExibeFonteRealComoRascunhoSemAutorizarCadastro() throws Exception {
		var resposta=cliente.send(HttpRequest.newBuilder(uri("/legal/terms-of-use")).GET().build(),HttpResponse.BodyHandlers.ofString());
		assertThat(resposta.statusCode()).isEqualTo(200);var dados=mapper.readTree(resposta.body());
		assertThat(dados.path("versao").asString()).isEqualTo("1.0.0");assertThat(dados.path("status").asString()).isEqualTo("RASCUNHO");
		assertThat(dados.path("aprovado").asBoolean()).isFalse();assertThat(dados.path("publicado").asBoolean()).isFalse();assertThat(dados.path("cadastroHabilitado").asBoolean()).isFalse();
		assertThat(dados.path("conteudo").asString()).contains("RASCUNHO");assertThat(dados.path("aprovadoEm").isNull()).isTrue();
		assertThat(resposta.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
	}
	@Test void backendBloqueiaCadastroAntesDeCriarContaMesmoComAceiteEChaveIdempotente() throws Exception {
		String corpo=mapper.writeValueAsString(Map.of("nome","Fictícia","email","ficticio@example.invalid","senha","Frase fictícia longa","crn","CRN-1 12345","aceitouTermos",true,"versaoTermos","1.0.0","status","PUBLICADO","aprovado",true,"publicado",true));
		var resposta=cliente.send(HttpRequest.newBuilder(uri("/api/v1/auth/cadastro-nutri")).header("Content-Type","application/json").header("Idempotency-Key",UUID.randomUUID().toString()).POST(HttpRequest.BodyPublishers.ofString(corpo)).build(),HttpResponse.BodyHandlers.ofString());
		assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(422);assertThat(mapper.readTree(resposta.body()).path("codigo").asString()).isEqualTo("TERMOS_INDISPONIVEIS");verifyNoInteractions(cadastro);
	}
	@Test void loginDeContaExistenteNaoEImpedidoPeloRascunho() throws Exception {
		var usuario=new UsuarioResposta(UUID.randomUUID(),"Fictício","ficticio@example.invalid",true,List.of(Perfil.PACIENTE),Perfil.PACIENTE,null,UUID.randomUUID(),"UTC");
		when(sessoes.entrar(any())).thenReturn(new SessaoResposta("TOKEN_FICTICIO_SEM_VALIDADE",null,Instant.now().plusSeconds(900),usuario));
		String corpo=mapper.writeValueAsString(Map.of("email","ficticio@example.invalid","senha","Frase fictícia longa","dispositivo",Map.of("id",UUID.randomUUID(),"plataforma","ANDROID","nomeAparelho","Teste")));
		var resposta=cliente.send(HttpRequest.newBuilder(uri("/api/v1/auth/login")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(corpo)).build(),HttpResponse.BodyHandlers.ofString());
		assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(200);verify(sessoes).entrar(any());
	}
	URI uri(String caminho){return URI.create("http://localhost:"+porta+caminho);}
}
