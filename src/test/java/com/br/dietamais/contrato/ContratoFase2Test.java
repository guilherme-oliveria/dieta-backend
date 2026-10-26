package com.br.dietamais.contrato;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import com.br.dietamais.comum.api.ProblemaResposta;

class ContratoFase2Test {
	final JsonMapper mapper=JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
	@Test void cobreAsDezesseisOperacoesSemDuplicidade() throws Exception {
		assertThat(manifesto()).hasSize(16);
		assertThat(manifesto().stream().map(e->e.metodo()+" "+e.caminho())).doesNotHaveDuplicates();
	}
	@TestFactory Stream<DynamicTest> exemplosSaoTipadosValidosEFieisAoJson() throws Exception {
		return manifesto().stream().flatMap(e->e.exemplos().stream()).map(e->DynamicTest.dynamicTest(e.arquivo(),()->{
			var json=recurso(e.arquivo());var tipo=mapper.getTypeFactory().constructFromCanonical(e.tipo());Object dto=mapper.readValue(json,tipo);
			try(var factory=Validation.buildDefaultValidatorFactory()){assertThat(factory.getValidator().validate(dto)).isEmpty();}
			assertThat(mapper.readTree(mapper.writeValueAsString(dto))).isEqualTo(mapper.readTree(json));
			if(dto instanceof ProblemaResposta erro){assertThat(erro.status()).isEqualTo(erro.codigo().status());if(erro.regra()!=null)assertThat(erro.codigo().regras()).contains(erro.regra());}
		}));
	}
	List<Endpoint> manifesto() throws Exception{return mapper.readValue(recurso("manifesto.json"),mapper.getTypeFactory().constructCollectionType(List.class,Endpoint.class));}
	@Test void validaConteudoAninhadoSemImpedirRascunhoVazio() throws Exception {
		var json=(tools.jackson.databind.node.ObjectNode)mapper.readTree(recurso("criarDieta/requisicao.json"));
		((tools.jackson.databind.node.ObjectNode)json.path("refeicoes").get(0).path("itens").get(0)).put("porcoes",0);
		var invalida=mapper.treeToValue(json,com.br.dietamais.dietas.api.SalvarDietaRequisicao.class);
		try(var factory=Validation.buildDefaultValidatorFactory()){
			assertThat(factory.getValidator().validate(invalida)).anySatisfy(erro->assertThat(erro.getPropertyPath().toString()).isEqualTo("refeicoes[0].itens[0].porcoes"));
			var rascunho=new com.br.dietamais.dietas.api.SalvarDietaRequisicao(null,"Rascunho fictício",null,List.of());
			assertThat(factory.getValidator().validate(rascunho)).isEmpty();
		}
	}
	String recurso(String nome) throws Exception {try(var input=getClass().getResourceAsStream("/contrato/fase-2/"+nome)){assertThat(input).as(nome).isNotNull();return new String(input.readAllBytes(),StandardCharsets.UTF_8);}}
	record Endpoint(String metodo,String caminho,int status,List<Exemplo> exemplos){}
	record Exemplo(String arquivo,String tipo){}
}
