package com.br.dietamais.questionarios;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.api.ExcecaoNegocio;
import com.br.dietamais.questionarios.api.*;
import com.br.dietamais.questionarios.dominio.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ValidadorRespostaTest {
	final JsonMapper mapper=JsonMapper.builder().build();
	PerguntaQuestionario p(TipoPergunta tipo,String... opcoes){return new PerguntaQuestionario(UUID.randomUUID(),"x",tipo,opcoes.length==0?null:List.of(opcoes),false,null);}
	boolean ok(PerguntaQuestionario p,String json){
		try{ValidadorResposta.validar(p,mapper.readTree(json));return true;}catch(ExcecaoNegocio e){return false;}
	}
	@Test void validaCadaTipo() {
		assertThat(ok(p(TipoPergunta.TEXTO),"\"ola\"")).isTrue();assertThat(ok(p(TipoPergunta.TEXTO),"\"  \"")).isFalse();assertThat(ok(p(TipoPergunta.TEXTO),"3")).isFalse();
		assertThat(ok(p(TipoPergunta.NUMERO),"7.5")).isTrue();assertThat(ok(p(TipoPergunta.NUMERO),"\"7\"")).isFalse();
		var unica=p(TipoPergunta.ESCOLHA_UNICA,"A","B");assertThat(ok(unica,"\"A\"")).isTrue();assertThat(ok(unica,"\"C\"")).isFalse();assertThat(ok(unica,"[\"A\"]")).isFalse();
		var multi=p(TipoPergunta.MULTIPLA_ESCOLHA,"A","B");assertThat(ok(multi,"[\"A\",\"B\"]")).isTrue();assertThat(ok(multi,"[\"A\",\"A\"]")).isFalse();assertThat(ok(multi,"[]")).isFalse();assertThat(ok(multi,"[\"Z\"]")).isFalse();
		var escala=p(TipoPergunta.ESCALA_1_5);assertThat(ok(escala,"1")).isTrue();assertThat(ok(escala,"5")).isTrue();assertThat(ok(escala,"6")).isFalse();assertThat(ok(escala,"0")).isFalse();assertThat(ok(escala,"2.5")).isFalse();
		var hora=p(TipoPergunta.HORARIO);assertThat(ok(hora,"\"07:30\"")).isTrue();assertThat(ok(hora,"\"24:00\"")).isFalse();assertThat(ok(hora,"\"7:30\"")).isFalse();
		var lista=p(TipoPergunta.LISTA_POR_REFEICAO);
		assertThat(ok(lista,"[{\"refeicao\":\"Café\",\"descricao\":\"Pão e café\",\"horario\":\"07:00\"},{\"refeicao\":\"Almoço\",\"descricao\":\"Arroz\"}]")).isTrue();
		assertThat(ok(lista,"[{\"refeicao\":\"Café\"}]")).isFalse();assertThat(ok(lista,"[{\"refeicao\":\"Café\",\"descricao\":\"x\",\"extra\":1}]")).isFalse();assertThat(ok(lista,"\"texto\"")).isFalse();
	}
	PerguntaQuestionario comCondicao(UUID ref,OperadorCondicao op,String valor){return new PerguntaQuestionario(UUID.randomUUID(),"x",TipoPergunta.TEXTO,null,false,new CondicaoPergunta(ref,op,valor));}
	@Test void condicaoDeExibicao() {
		var ref=UUID.randomUUID();
		var sem=Map.<UUID,tools.jackson.databind.JsonNode>of();var fem=Map.<UUID,tools.jackson.databind.JsonNode>of(ref,mapper.readTree("\"Feminino\""));
		var multi=Map.<UUID,tools.jackson.databind.JsonNode>of(ref,mapper.readTree("[\"A\",\"B\"]"));
		assertThat(ValidadorResposta.visivel(comCondicao(ref,OperadorCondicao.IGUAL,"Feminino"),sem)).isFalse();
		assertThat(ValidadorResposta.visivel(comCondicao(ref,OperadorCondicao.IGUAL,"Feminino"),fem)).isTrue();
		assertThat(ValidadorResposta.visivel(comCondicao(ref,OperadorCondicao.DIFERENTE,"Feminino"),sem)).isTrue();
		assertThat(ValidadorResposta.visivel(comCondicao(ref,OperadorCondicao.DIFERENTE,"Feminino"),fem)).isFalse();
		assertThat(ValidadorResposta.visivel(comCondicao(ref,OperadorCondicao.CONTEM,"B"),multi)).isTrue();
		assertThat(ValidadorResposta.visivel(comCondicao(ref,OperadorCondicao.CONTEM,"C"),multi)).isFalse();
		assertThat(ValidadorResposta.visivel(new PerguntaQuestionario(UUID.randomUUID(),"x",TipoPergunta.TEXTO,null,false,null),sem)).isTrue();
	}
}
