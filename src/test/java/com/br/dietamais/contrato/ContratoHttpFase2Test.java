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
		OpenApiConfig.class,SecurityConfig.class,FabricaProblema.class,com.br.dietamais.comum.api.TratadorErros.class})
	static class Aplicacao {}
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	org.springframework.security.oauth2.jwt.JwtDecoder decoder;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.br.dietamais.contas.aplicacao.GerenciarPerfilNutri perfil;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.br.dietamais.alimentos.aplicacao.ConsultarGruposAlimentos grupos;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.br.dietamais.alimentos.aplicacao.GerenciarAlimentos alimentos;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.br.dietamais.alimentos.aplicacao.GerenciarListaPaciente listas;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.br.dietamais.dietas.aplicacao.GerenciarDietas dietas;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.br.dietamais.dietas.aplicacao.PublicarDietas publicacao;
	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.br.dietamais.dietas.aplicacao.ConsultarDietasPaciente minhasDietas;
	@Autowired WebApplicationContext contexto;
	@Autowired JsonMapper mapper;
	MockMvc mvc;
	@BeforeEach void configurar(){mvc=webAppContextSetup(contexto).apply(springSecurity()).build();}
	@Test @WithMockUser void todasAsRotasRetornam501TipadoSemBanco() throws Exception {
		for(var e:mapper.readTree(recurso("manifesto.json"))){
			if(implementado(e.get("caminho").asString()))continue;
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
			assertThat(op.path("responses").has("501")).isEqualTo(!implementado(e.get("caminho").asString()));
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
	boolean implementado(String caminho){return java.util.Set.of("/api/v1/eu/nutri","/api/v1/grupos-alimentos","/api/v1/grupos-alimentos/{id}","/api/v1/alimentos","/api/v1/alimentos/{id}","/api/v1/pacientes/{id}/lista-substituicao","/api/v1/pacientes/{id}/dietas","/api/v1/dietas/{id}","/api/v1/dietas/{id}/publicar","/api/v1/dietas/{id}/duplicar","/api/v1/eu/dietas","/api/v1/eu/dieta-em-uso").contains(caminho);}
	@Test @WithMockUser void gruposImplementadosRetornam200SemCache() throws Exception {
		var resposta=mapper.readValue(recurso("listarGruposAlimentos/resposta.json"),com.br.dietamais.alimentos.api.GruposAlimentosResposta.class);
		org.mockito.Mockito.when(grupos.executar()).thenReturn(resposta);
		mvc.perform(get("/api/v1/grupos-alimentos")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
			.andExpect(jsonPath("$.itens[0].kcalPorPorcao").value(150));
	}
	@Test @WithMockUser void alimentosMacrosEListaDoPacienteJaSaoOperacoesReais() throws Exception {
		var alimento=mapper.readValue(recurso("criarAlimento/resposta.json"),com.br.dietamais.alimentos.api.AlimentoResposta.class);
		var edicao=mapper.readValue(recurso("atualizarAlimento/resposta.json"),com.br.dietamais.alimentos.api.AlimentoResposta.class);
		var pagina=mapper.readValue(recurso("listarAlimentos/resposta.json"),com.br.dietamais.alimentos.api.PaginaAlimentosResposta.class);
		var grupo=mapper.readValue(recurso("atualizarMacrosGrupo/resposta.json"),com.br.dietamais.alimentos.api.GrupoAlimentoResposta.class);
		var lista=mapper.readValue(recurso("consultarListaSubstituicao/resposta.json"),com.br.dietamais.alimentos.api.ListaSubstituicaoResposta.class);
		org.mockito.Mockito.when(alimentos.criar(org.mockito.ArgumentMatchers.any())).thenReturn(alimento);
		org.mockito.Mockito.when(alimentos.listar(0,20,null,null,true)).thenReturn(pagina);
		org.mockito.Mockito.when(alimentos.atualizar(org.mockito.ArgumentMatchers.eq(edicao.id()),org.mockito.ArgumentMatchers.eq("\"1\""),org.mockito.ArgumentMatchers.any())).thenReturn(edicao);
		org.mockito.Mockito.when(alimentos.atualizarMacros(org.mockito.ArgumentMatchers.eq(grupo.id()),org.mockito.ArgumentMatchers.eq("\"1\""),org.mockito.ArgumentMatchers.any())).thenReturn(grupo);
		org.mockito.Mockito.when(listas.consultar(lista.pacienteId())).thenReturn(lista);
		org.mockito.Mockito.when(listas.atualizar(org.mockito.ArgumentMatchers.eq(lista.pacienteId()),org.mockito.ArgumentMatchers.eq("\"1\""),org.mockito.ArgumentMatchers.any())).thenReturn(lista);
		mvc.perform(get("/api/v1/alimentos")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
		mvc.perform(post("/api/v1/alimentos").contentType("application/json").content(recurso("criarAlimento/requisicao.json"))).andExpect(status().isCreated()).andExpect(header().string("ETag","\"1\""));
		mvc.perform(patch("/api/v1/alimentos/"+edicao.id()).header("If-Match","\"1\"").contentType("application/json").content(recurso("atualizarAlimento/requisicao.json"))).andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false)).andExpect(header().string("ETag","\"2\""));
		mvc.perform(patch("/api/v1/grupos-alimentos/"+grupo.id()).header("If-Match","\"1\"").contentType("application/json").content(recurso("atualizarMacrosGrupo/requisicao.json"))).andExpect(status().isOk()).andExpect(jsonPath("$.origemMacros").value("AJUSTE_NUTRI"));
		mvc.perform(get("/api/v1/pacientes/"+lista.pacienteId()+"/lista-substituicao")).andExpect(status().isOk()).andExpect(header().string("ETag","\"1\""));
		mvc.perform(put("/api/v1/pacientes/"+lista.pacienteId()+"/lista-substituicao").header("If-Match","\"1\"").contentType("application/json").content(recurso("atualizarListaSubstituicao/requisicao.json"))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
	}
	@Test @WithMockUser void transporteRejeitaPaginaPrecisaoENumerosInvalidosAntesDaEscrita() throws Exception {
		mvc.perform(get("/api/v1/alimentos?pagina=-1")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("FORMATO_INVALIDO"));
		String caminho="/api/v1/grupos-alimentos/019a0000-0000-7000-8000-000000000001";
		mvc.perform(patch(caminho).contentType("application/json").content(recurso("atualizarMacrosGrupo/requisicao.json"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("FORMATO_INVALIDO"));
		mvc.perform(patch("/api/v1/eu/nutri").contentType("application/json").content(recurso("atualizarPerfilNutri/requisicao.json"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("FORMATO_INVALIDO"));
		org.mockito.Mockito.verify(perfil,org.mockito.Mockito.never()).atualizar(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
		var json=(tools.jackson.databind.node.ObjectNode)mapper.readTree(recurso("atualizarMacrosGrupo/requisicao.json"));json.put("carboidratosPorPorcao",-1);
		mvc.perform(patch(caminho).header("If-Match","\"0\"").contentType("application/json").content(mapper.writeValueAsString(json))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("FORMATO_INVALIDO"));
		json.put("carboidratosPorPorcao",0.00001);
		mvc.perform(patch(caminho).header("If-Match","\"0\"").contentType("application/json").content(mapper.writeValueAsString(json))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("FORMATO_INVALIDO"));
		org.mockito.Mockito.verify(alimentos,org.mockito.Mockito.never()).atualizarMacros(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
	}
}
