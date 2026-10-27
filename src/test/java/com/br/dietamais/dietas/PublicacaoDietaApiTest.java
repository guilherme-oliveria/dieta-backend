package com.br.dietamais.dietas;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.TesteIsolamento;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class PublicacaoDietaApiTest extends TesteIsolamento {
	static final List<com.br.dietamais.dietas.api.DietaPublicada> EVENTOS=new java.util.concurrent.CopyOnWriteArrayList<>();
	@org.springframework.boot.test.context.TestConfiguration static class Captura {
		@org.springframework.context.event.EventListener void capturar(com.br.dietamais.dietas.api.DietaPublicada e){EVENTOS.add(e);}
	}
	@Test void publicarCongelaDietaListaEOrientacaoEEmiteEvento() throws Exception {
		var a=cadastrarNutri();var p=criarPaciente(a.token());String pid=p.path("id").asString();var ali=alimentos(a.token(),2);
		jdbc.update("update nutricionista set orientacao_padrao='Orientação padrão fictícia' where id=?",a.nutri());
		var d=criar(a.token(),pid,dieta("Plano",refeicao("Café",0,item(ali.get(0),"2")),refeicao("Vazia",1),refeicao("Lanche",2,item(ali.get(1),"1"))));String id=d.path("id").asString();
		var publicada=requisitar("POST","/dietas/"+id+"/publicar",a.token(),null,Map.of("If-Match","\"1\""));assertThat(publicada.statusCode()).as(publicada.body()).isEqualTo(200);
		var s=json(publicada);assertThat(s.path("dietaId").asString()).isEqualTo(id);assertThat(s.path("orientacao").asString()).isEqualTo("Orientação padrão fictícia");
		assertThat(s.path("refeicoes")).hasSize(2);assertThat(s.path("totais").path("kcal").decimalValue()).isEqualByComparingTo(d.path("totais").path("kcal").decimalValue());
		assertThat(s.path("nutricionista").path("id").asString()).isEqualTo(a.nutri().toString());assertThat(s.path("nutricionista").path("crn").asString()).isNotBlank();
		assertThat(s.path("listaSubstituicao").path("pacienteId").asString()).isEqualTo(pid);assertThat(s.path("listaSubstituicao").path("alimentos").size()).isGreaterThan(100);
		var depois=json(requisitar("GET","/dietas/"+id,a.token(),null));assertThat(depois.path("status").asString()).isEqualTo("PUBLICADA");assertThat(depois.path("versao").asLong()).isEqualTo(2);
		assertThat(EVENTOS.stream().filter(e->e.dietaId().toString().equals(id)).count()).isEqualTo(1);
		// congela: mudar alimento e lista depois não altera o snapshot
		jdbc.update("update alimento_nutri set nome='Renomeado depois', ativo=false where id=?",UUID.fromString(ali.get(0).path("id").asString()));
		var gravado=jdbc.queryForObject("select conteudo::text from dieta_snapshot where dieta_id=?",String.class,UUID.fromString(id));
		assertThat(gravado).contains(ali.get(0).path("nome").asString()).doesNotContain("Renomeado depois");
		assertThatThrownBy(()->jdbc.update("update dieta_snapshot set conteudo='{}'::jsonb where dieta_id=?",UUID.fromString(id))).hasMessageContaining("imutável");
		erro(requisitar("POST","/dietas/"+id+"/publicar",a.token(),null,Map.of("If-Match","\"2\"")),422,"DIETA_NAO_EDITAVEL");
	}
	@Test void publicarSubstituiAAnteriorERecusaVaziaEConflito() throws Exception {
		var a=cadastrarNutri();var p=criarPaciente(a.token());String pid=p.path("id").asString();var ali=alimentos(a.token(),1);
		var d1=criar(a.token(),pid,dieta("Primeira",refeicao("R",0,item(ali.get(0),"1"))));var d2=criar(a.token(),pid,dieta("Segunda",refeicao("R",0,item(ali.get(0),"2"))));
		var vazia=criar(a.token(),pid,dieta("Vazia",refeicao("R",0)));
		erro(requisitar("POST","/dietas/"+vazia.path("id").asString()+"/publicar",a.token(),null,Map.of("If-Match","\"1\"")),422,"DIETA_VAZIA");
		var conflito=requisitar("POST","/dietas/"+d1.path("id").asString()+"/publicar",a.token(),null,Map.of("If-Match","\"9\""));erro(conflito,409,"CONFLITO_VERSAO");assertThat(json(conflito).path("atual").path("titulo").asString()).isEqualTo("Primeira");
		assertThat(requisitar("POST","/dietas/"+d1.path("id").asString()+"/publicar",a.token(),null,Map.of("If-Match","\"1\"")).statusCode()).isEqualTo(200);
		assertThat(requisitar("POST","/dietas/"+d2.path("id").asString()+"/publicar",a.token(),null,Map.of("If-Match","\"1\"")).statusCode()).isEqualTo(200);
		assertThat(json(requisitar("GET","/dietas/"+d1.path("id").asString(),a.token(),null)).path("status").asString()).isEqualTo("SUBSTITUIDA");
		assertThat(json(requisitar("GET","/dietas/"+d2.path("id").asString(),a.token(),null)).path("status").asString()).isEqualTo("PUBLICADA");
		assertThat(jdbc.queryForObject("select count(*) from dieta where vinculo_id=? and status='PUBLICADA'",Integer.class,UUID.fromString(p.path("vinculoId").asString()))).isEqualTo(1);
		assertThat(jdbc.queryForObject("select count(*) from dieta_snapshot where vinculo_id=?",Integer.class,UUID.fromString(p.path("vinculoId").asString()))).isEqualTo(2);
	}
	@Test void duplicarCriaRascunhoNovoEPublicacaoRespeitaIsolamentoAssinaturaEVinculo() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var p=criarPaciente(a.token());String pid=p.path("id").asString();var ali=alimentos(a.token(),1);
		var d=criar(a.token(),pid,dieta("Original",refeicao("R",0,item(ali.get(0),"1.5"))));String id=d.path("id").asString();
		erro(requisitar("POST","/dietas/"+id+"/publicar",b.token(),null,Map.of("If-Match","\"1\"")),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("POST","/dietas/"+id+"/duplicar",b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		assertThat(requisitar("POST","/dietas/"+id+"/publicar",a.token(),null,Map.of("If-Match","\"1\"")).statusCode()).isEqualTo(200);
		var copia=requisitar("POST","/dietas/"+id+"/duplicar",a.token(),null);assertThat(copia.statusCode()).as(copia.body()).isEqualTo(201);var c=json(copia);
		assertThat(c.path("id").asString()).isNotEqualTo(id);assertThat(c.path("status").asString()).isEqualTo("RASCUNHO");assertThat(c.path("titulo").asString()).isEqualTo("Original (cópia)");assertThat(c.path("versao").asLong()).isEqualTo(1);
		assertThat(c.path("refeicoes").get(0).path("id").asString()).isNotEqualTo(d.path("refeicoes").get(0).path("id").asString());
		assertThat(c.path("totais")).isEqualTo(d.path("totais"));
		jdbc.update("update assinatura set status='CANCELADA' where organizacao_id=?",a.organizacao());
		erro(requisitar("POST","/dietas/"+c.path("id").asString()+"/publicar",a.token(),null,Map.of("If-Match","\"1\"")),422,"ASSINATURA_INATIVA");
		erro(requisitar("POST","/dietas/"+id+"/duplicar",a.token(),null),422,"ASSINATURA_INATIVA");
		jdbc.update("update assinatura set status='TESTE' where organizacao_id=?",a.organizacao());
		assertThat(requisitar("POST","/pacientes/"+pid+"/encerrar-vinculo",a.token(),null).statusCode()).isEqualTo(200);
		erro(requisitar("POST","/dietas/"+c.path("id").asString()+"/publicar",a.token(),null,Map.of("If-Match","\"1\"")),403,"ACESSO_NEGADO");
		erro(requisitar("POST","/dietas/"+id+"/duplicar",a.token(),null),403,"ACESSO_NEGADO");
	}
	JsonNode criarPaciente(String token) throws Exception {var r=requisitar("POST","/pacientes",token,Map.of("nome","Paciente fictício","email",UUID.randomUUID()+"@example.invalid"));assertThat(r.statusCode()).isEqualTo(201);return json(r);}
	List<JsonNode> alimentos(String token,int quantidade) throws Exception {
		var pagina=json(requisitar("GET","/alimentos?tamanho="+quantidade,token,null));var lista=new ArrayList<JsonNode>();pagina.path("itens").forEach(lista::add);return lista;
	}
	JsonNode criar(String token,String pid,Map<String,Object> corpo) throws Exception {var r=requisitar("POST","/pacientes/"+pid+"/dietas",token,corpo);assertThat(r.statusCode()).as(r.body()).isEqualTo(201);return json(r);}
	Map<String,Object> dieta(String titulo,Object... refeicoes){var m=new HashMap<String,Object>();m.put("titulo",titulo);m.put("refeicoes",new ArrayList<>(List.of(refeicoes)));return m;}
	Map<String,Object> refeicao(String nome,int ordem,Object... itens){var m=new HashMap<String,Object>();m.put("nome",nome);m.put("horario","08:00:00");m.put("ordem",ordem);m.put("itens",new ArrayList<>(List.of(itens)));return m;}
	Map<String,Object> item(JsonNode alimento,String porcoes){var m=new HashMap<String,Object>();m.put("alimentoId",alimento.path("id").asString());m.put("porcoes",new BigDecimal(porcoes));return m;}
}
