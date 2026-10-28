package com.br.dietamais.contrato;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.config.SecurityConfig;
import com.br.dietamais.contas.api.*;
import com.br.dietamais.contas.aplicacao.GerenciarPerfilNutri;
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
import org.springframework.security.oauth2.jwt.*;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes=ContratoIfMatchHttpTest.Aplicacao.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
	"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration","spring.docker.compose.enabled=false","springdoc.swagger-ui.enabled=false"})
class ContratoIfMatchHttpTest {
	@Configuration(proxyBeanMethods=false) @EnableAutoConfiguration
	@Import({SecurityConfig.class,PerfilNutriController.class,FabricaProblema.class,TratadorErros.class})
	static class Aplicacao{}
	@MockitoBean GerenciarPerfilNutri perfil;
	@MockitoBean com.br.dietamais.contas.aplicacao.GerenciarLogoNutri logo;
	@MockitoBean JwtDecoder decoder;
	@LocalServerPort int porta;
	@Autowired JsonMapper mapper;
	final HttpClient cliente=HttpClient.newHttpClient();
	@AfterEach void fechar(){cliente.close();}
	@Test void servidorHttpRealRejeitaIfMatchAusenteSemChamarEscritaEAceitaQuandoInformado() throws Exception {
		// Decoder controlado: não existe credencial real; MVC, segurança, binding e advice são reais.
		String credencial="FICTICIA_SEM_VALIDADE";var agora=Instant.now();
		when(decoder.decode(credencial)).thenReturn(Jwt.withTokenValue(credencial).header("alg","RS256").subject(UUID.randomUUID().toString()).claim("perfil","NUTRICIONISTA").issuedAt(agora).expiresAt(agora.plusSeconds(60)).build());
		String corpo=mapper.writeValueAsString(Map.of("nome","Nutri fictícia","crn","CRN-1 00000"));
		var uri=URI.create("http://localhost:"+porta+"/api/v1/eu/nutri");
		var semHeader=cliente.send(HttpRequest.newBuilder(uri).header("Authorization","Bearer "+credencial).header("Content-Type","application/json").method("PATCH",HttpRequest.BodyPublishers.ofString(corpo)).build(),HttpResponse.BodyHandlers.ofString());
		assertThat(semHeader.statusCode()).as(semHeader.body()).isEqualTo(400);assertThat(mapper.readTree(semHeader.body()).path("codigo").asString()).isEqualTo("FORMATO_INVALIDO");
		verifyNoInteractions(perfil);
		when(perfil.atualizar(eq("\"0\""),any())).thenReturn(new PerfilNutriResposta(UUID.randomUUID(),"Nutri fictícia","CRN-1 00000",null,null,false,1L,Instant.EPOCH));
		var comHeader=cliente.send(HttpRequest.newBuilder(uri).header("Authorization","Bearer "+credencial).header("Content-Type","application/json").header("If-Match","\"0\"").method("PATCH",HttpRequest.BodyPublishers.ofString(corpo)).build(),HttpResponse.BodyHandlers.ofString());
		assertThat(comHeader.statusCode()).as(comHeader.body()).isEqualTo(200);assertThat(comHeader.headers().firstValue("ETag").orElseThrow()).isEqualTo("\"1\"");
		verify(perfil).atualizar("\"0\"",new AtualizarPerfilNutriRequisicao("Nutri fictícia","CRN-1 00000",null,null));
	}
}
