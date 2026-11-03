package com.br.dietamais.contrato;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.comum.api.ProblemaResposta;

/** BE-C7: exemplos do contrato da fase 7 e formato dos vetores de teste compartilhados com o mobile (RN-CAL-06). */
class ContratoFase7Test {
	static final Set<String> FUNCOES=Set.of("avaliacaoAdulto","classificarImcAdulto","classificarImcIdoso","classificarCintura",
			"classificarCompleicao","classificarPercentualGordura","calcularIdade");
	final JsonMapper mapper=JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

	record Exemplo(String arquivo,Class<?> tipo){}
	static Stream<Exemplo> exemplos(){
		return Stream.of(new Exemplo("avaliacaoAmpliada-resposta.json",AvaliacaoResposta.class),
				new Exemplo("avaliacaoAmpliada-requisicao.json",SalvarAvaliacaoRequisicao.class),
				new Exemplo("protocolosNutri-resposta.json",ProtocolosNutriResposta.class),
				new Exemplo("protocolosNutri-requisicao.json",SalvarProtocolosNutriRequisicao.class),
				new Exemplo("erro-dado-insuficiente.json",ProblemaResposta.class),
				new Exemplo("erro-protocolo-invalido.json",ProblemaResposta.class));
	}
	@TestFactory Stream<DynamicTest> exemplosSaoTipadosValidosEFieisAoJson(){
		return exemplos().map(e->DynamicTest.dynamicTest(e.arquivo(),()->{
			var json=recurso("/contrato/fase-7/"+e.arquivo());Object dto=mapper.readValue(json,e.tipo());
			try(var factory=Validation.buildDefaultValidatorFactory()){assertThat(factory.getValidator().validate(dto)).isEmpty();}
			// requisições aceitam campos omitidos; só respostas precisam voltar idênticas
			if(!e.arquivo().contains("requisicao"))assertThat(mapper.readTree(mapper.writeValueAsString(dto))).isEqualTo(mapper.readTree(json));
			if(dto instanceof ProblemaResposta erro){assertThat(erro.status()).isEqualTo(erro.codigo().status());assertThat(erro.codigo().regras()).contains(erro.regra());}
		}));
	}
	@Test void vetoresDeAdultoTemFormatoConhecidoEOrigem() throws Exception {
		var raiz=mapper.readTree(recurso("/contrato/calculos/adulto.json"));
		assertThat(raiz.path("versao").asString()).isNotBlank();
		var ids=new HashSet<String>();
		for(JsonNode caso:raiz.path("casos")){
			assertThat(ids.add(caso.path("id").asString())).as("id repetido "+caso.path("id")).isTrue();
			assertThat(FUNCOES).contains(caso.path("funcao").asString());
			assertThat(caso.path("origem").asString()).as(caso.path("id").asString()).isNotBlank();
			assertThat(caso.has("entrada")&&caso.has("esperado")).isTrue();
		}
		assertThat(ids).hasSizeGreaterThan(50).contains("V01-homem-29-sete-dobras");
	}
	String recurso(String caminho) throws Exception {try(var input=getClass().getResourceAsStream(caminho)){assertThat(input).as(caminho).isNotNull();return new String(input.readAllBytes(),StandardCharsets.UTF_8);}}
}
