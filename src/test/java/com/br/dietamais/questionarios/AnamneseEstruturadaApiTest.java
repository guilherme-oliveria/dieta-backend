package com.br.dietamais.questionarios;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class AnamneseEstruturadaApiTest extends TesteComPaciente {
	JsonNode modelo(String token) throws Exception {
		return json(requisitar("GET","/modelos-questionario",token,null)).path("itens").get(0);
	}
	String codigo(JsonNode q,String codigo){
		for(var b:q.path("blocos"))for(var p:b.path("perguntas"))if(codigo.equals(p.path("codigo").asString()))return p.path("id").asString();
		throw new AssertionError(codigo);
	}
	@Test void calculaNoRestNoSyncECongelaAoConcluirComIsolamento() throws Exception {
		var n=cadastrarNutri();var p=pacienteVinculado(n);var outro=pacienteVinculado(n);var outraNutri=cadastrarNutri();var m=modelo(n.token());
		assertThat(m.path("nome").asString()).isEqualTo("Anamnese completa");
		var frequencia=new ArrayList<JsonNode>();
		for(var bloco:m.path("blocos"))if(bloco.path("titulo").asString().equals("Frequência alimentar"))bloco.path("perguntas").forEach(frequencia::add);
		assertThat(frequencia).hasSize(20);assertThat(frequencia.get(0).path("texto").asString()).isEqualTo("Arroz, vegetal C e similares");
		assertThat(frequencia.get(0).path("opcoes")).hasSize(7);assertThat(frequencia.get(19).path("texto").asString()).isEqualTo("Produtos diet & light");
		var envio=requisitar("POST","/pacientes/"+p.fichaId()+"/questionarios",n.token(),Map.of("modeloId",m.path("id").asString()));
		assertThat(envio.statusCode()).as(envio.body()).isEqualTo(201);var q=json(envio);var id=q.path("id").asString();
		var novoModelo=Map.of("nome","Modelo alterado","blocos",List.of(Map.of("titulo","Novo","perguntas",List.of(Map.of("texto","Texto sem códigos","tipo","TEXTO")))));
		assertThat(requisitar("PUT","/modelos-questionario/"+m.path("id").asString(),n.token(),novoModelo,Map.of("If-Match","\"1\"" )).statusCode()).isEqualTo(200);
		var congelado=json(requisitar("GET","/eu/questionarios",p.token(),null)).path("itens").get(0);
		assertThat(codigo(congelado,"SAL_PESSOAS")).isEqualTo(codigo(q,"SAL_PESSOAS"));
		var respostas=new LinkedHashMap<String,Object>();respostas.put(codigo(q,"PA_SISTOLICA"),140);respostas.put(codigo(q,"PA_DIASTOLICA"),80);
		respostas.put(codigo(q,"SAL_GRAMAS_MES"),650);respostas.put(codigo(q,"SAL_PESSOAS"),3);
		var parcial=requisitar("PUT","/eu/questionarios/"+id+"/respostas",p.token(),Map.of("respostas",respostas));
		assertThat(parcial.statusCode()).as(parcial.body()).isEqualTo(200);
		assertThat(json(parcial).path("derivados").path("pressaoArterial").asString()).isEqualTo("HIPERTENSAO_SISTOLICA_ISOLADA");
		assertThat(json(parcial).path("derivados").path("salGramasPorPessoaDia").asInt()).isEqualTo(7);
		var invalidas=Map.of("respostas",Map.of(codigo(q,"SAL_PESSOAS"),0));
		erro(requisitar("PUT","/eu/questionarios/"+id+"/respostas",p.token(),invalidas),400,"FORMATO_INVALIDO");
		erro(requisitar("PUT","/eu/questionarios/"+id+"/respostas",outro.token(),Map.of("respostas",respostas)),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("GET","/pacientes/"+p.fichaId()+"/questionarios",outraNutri.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		// Sync usa a mesma validação e recalcula após a mescla.
		var lote=Map.of("mutacoes",List.of(Map.of("idMutacao",UUID.randomUUID().toString(),"entidade","questionarios","operacao","ATUALIZAR","id",id,"dados",Map.of("respostas",Map.of(codigo(q,"SAL_GRAMAS_MES"),900)))));
		var sync=requisitar("POST","/sync",p.token(),lote);assertThat(json(sync).path("resultados").get(0).path("status").asString()).isEqualTo("APLICADA");
		var dados=json(requisitar("GET","/sync?limite=1000",p.token(),null)).path("mudancas").path("questionarios").get(0).path("dados");
		assertThat(dados.path("derivados").path("salGramasPorPessoaDia").asInt()).isEqualTo(10);
		// Apagar uma entrada limpa seu derivado.
		var apagar=new HashMap<String,Object>();apagar.put(codigo(q,"SAL_PESSOAS"),null);
		var apagado=json(requisitar("PUT","/eu/questionarios/"+id+"/respostas",p.token(),Map.of("respostas",apagar)));
		assertThat(apagado.path("derivados").path("salGramasPorPessoaDia").isNull()).isTrue();
		var obrigatorias=new HashMap<String,Object>();
		for(var b:q.path("blocos"))for(var pergunta:b.path("perguntas"))if(pergunta.path("obrigatoria").asBoolean())obrigatorias.put(pergunta.path("id").asString(),pergunta.path("opcoes").get(0).asString());
		assertThat(requisitar("PUT","/eu/questionarios/"+id+"/respostas",p.token(),Map.of("respostas",obrigatorias)).statusCode()).isEqualTo(200);
		var concluido=requisitar("POST","/eu/questionarios/"+id+"/enviar",p.token(),null);assertThat(concluido.statusCode()).as(concluido.body()).isEqualTo(200);
		assertThat(json(concluido).path("derivados").path("pressaoArterial").asString()).isEqualTo("HIPERTENSAO_SISTOLICA_ISOLADA");
		erro(requisitar("PUT","/eu/questionarios/"+id+"/respostas",p.token(),Map.of("respostas",respostas)),409,"QUESTIONARIO_FECHADO");
		assertThatThrownBy(()->jdbc.update("update questionario set derivados='{}'::jsonb where id=?::uuid",id)).hasMessageContaining("imutáveis");
		var lido=json(requisitar("GET","/pacientes/"+p.fichaId()+"/questionarios",n.token(),null)).path("itens").get(0);
		assertThat(lido.path("derivados")).isEqualTo(json(concluido).path("derivados"));
	}
	@Test void preservaModeloAntigoEditadoECriaCompletoUmaVezMesmoRenomeado() throws Exception {
		var n=cadastrarNutri();
		jdbc.update("insert into modelo_questionario(nutricionista_id,nome,padrao) values(?,?,true)",n.nutri(),"Modelo editado antigo");
		var itens=json(requisitar("GET","/modelos-questionario",n.token(),null)).path("itens");assertThat(itens).hasSize(2);
		var completa=itens.get(1);assertThat(completa.path("nome").asString()).isEqualTo("Anamnese completa");
		var edicao=Map.of("nome","Meu completo","blocos",mapper.convertValue(completa.path("blocos"),Object.class));
		var alterada=requisitar("PUT","/modelos-questionario/"+completa.path("id").asString(),n.token(),edicao,Map.of("If-Match","\"1\""));assertThat(alterada.statusCode()).as(alterada.body()).isEqualTo(200);
		assertThat(json(requisitar("GET","/modelos-questionario",n.token(),null)).path("itens")).hasSize(2);
		assertThat(codigo(json(alterada),"SAL_PESSOAS")).isEqualTo(codigo(completa,"SAL_PESSOAS"));
	}
}
