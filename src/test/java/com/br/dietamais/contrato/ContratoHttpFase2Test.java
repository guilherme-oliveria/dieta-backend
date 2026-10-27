package com.br.dietamais.contrato;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import java.nio.charset.StandardCharsets;
import com.br.dietamais.config.*;
import com.br.dietamais.alimentos.api.AlimentoController;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.contas.api.PerfilNutriController;
import com.br.dietamais.comum.api.FabricaProblema;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes=ContratoHttpFase2Test.Aplicacao.class,properties={
	"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
	"spring.docker.compose.enabled=false","springdoc.swagger-ui.enabled=false"})
class ContratoHttpFase2Test {
	@Configuration(proxyBeanMethods=false) @EnableAutoConfiguration
	@Import({AlimentoController.class,DietaController.class,DietaPacienteController.class,PerfilNutriController.class,
		OpenApiConfig.class,SecurityConfig.class,FabricaProblema.class})
	static class Aplicacao {}
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	org.springframework.security.oauth2.jwt.JwtDecoder decoder;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.br.dietamais.contas.aplicacao.GerenciarPerfilNutri perfil;
	@Autowired WebApplicationContext contexto;
	@Autowired JsonMapper mapper;
	MockMvc mvc;
	@BeforeEach void configurar(){mvc=webAppContextSetup(contexto).apply(springSecurity()).build();}
	@Test @WithMockUser void todasAsRotasRetornam501TipadoSemBanco() throws Exception {
		for(var e:mapper.readTree(recurso("manifesto.json"))){
			if(e.get("caminho").asString().equals("/api/v1/eu/nutri"))continue;
			var req=request(HttpMethod.valueOf(e.get("metodo").asString()),e.get("caminho").asString().replace("{id}","019a0f32-5000-7000-8000-000000000003")).header("If-Match","\"1\"");
			for(var exemplo:e.get("exemplos"))if(exemplo.get("arquivo").asString().endsWith("/requisicao.json"))req.contentType("application/json").content(recurso(exemplo.get("arquivo").asString()));
			var res=mvc.perform(req).andExpect(status().isNotImplemented()).andExpect(content().contentTypeCompatibleWith("application/problem+json")).andReturn().getResponse();
			assertThat(mapper.readTree(res.getContentAsString(StandardCharsets.UTF_8))).isEqualTo(mapper.readTree(recurso("nao-implementado.json")));
		}
	}
	@Test @WithMockUser void openApiPublicaSucessoPrevistoErrosSchemasECamposVersionados() throws Exception {
		var api=mapper.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
		for(var e:mapper.readTree(recurso("manifesto.json"))){
			var op=api.path("paths").path(e.get("caminho").asString()).path(e.get("metodo").asString().toLowerCase(java.util.Locale.ROOT));
			assertThat(op.isMissingNode()).isFalse();
			for(String status:new String[]{e.get("status").asString(),"400","401","403","404","409","422"})assertThat(op.path("responses").has(status)).as(e.toString()+status).isTrue();
			assertThat(op.path("responses").has("501")).isEqualTo(!e.get("caminho").asString().equals("/api/v1/eu/nutri"));
			assertThat(op.path("security").toString()).contains("bearerAuth");
		}
		var schemas=api.path("components").path("schemas");
		for(String tipo:new String[]{"DietaResposta","DietaSnapshotResposta","ListaSubstituicaoResposta","PerfilNutriResposta","TotaisNutricionaisResposta"})assertThat(schemas.has(tipo)).isTrue();
		assertThat(schemas.path("SalvarDietaRequisicao").path("properties").has("totais")).isFalse();
	}
	@Test void contratoContinuaProtegidoPorBearer() throws Exception {
		mvc.perform(get("/api/v1/grupos-alimentos")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
		mvc.perform(get("/api/v1/eu/dietas").header("Authorization","Basic ZmljdGljaW86U0VNX1ZBTElEQURF")).andExpect(status().isUnauthorized());
	}
	@Test @WithMockUser void perfilJaImplementadoRespondeComETagENoStore() throws Exception {
		var resposta=mapper.readValue(recurso("consultarPerfilNutri/resposta.json"),com.br.dietamais.contas.api.PerfilNutriResposta.class);
		org.mockito.Mockito.when(perfil.consultar()).thenReturn(resposta);
		org.mockito.Mockito.when(perfil.atualizar(org.mockito.ArgumentMatchers.eq("\"1\""),org.mockito.ArgumentMatchers.any())).thenReturn(resposta);
		mvc.perform(get("/api/v1/eu/nutri")).andExpect(status().isOk()).andExpect(header().string("ETag","\"1\"")).andExpect(header().string("Cache-Control","no-store"));
		mvc.perform(patch("/api/v1/eu/nutri").header("If-Match","\"1\"").contentType("application/json").content(recurso("atualizarPerfilNutri/requisicao.json")))
			.andExpect(status().isOk()).andExpect(header().string("ETag","\"1\"")).andExpect(header().string("Cache-Control","no-store"));
	}
	String recurso(String nome) throws Exception {try(var in=getClass().getResourceAsStream("/contrato/fase-2/"+nome)){assertThat(in).isNotNull();return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
}
