package com.br.dietamais.dietas;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.TesteIsolamento;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class DietasApiTest extends TesteIsolamento {
	static final AtomicLong IDS=new AtomicLong(5000);
	@Test void rascunhoCalculaTotaisNoBackendEIgnoraValoresDoCliente() throws Exception {
		var a=cadastrarNutri();var p=criarPaciente(a.token());String pid=p.path("id").asString();var ali=alimentos(a.token(),2);
		var primeiro=ali.get(0);var segundo=ali.get(1);
		var corpo=dieta("Dieta fictícia",refeicao("Café",0,item(primeiro,"2"),item(segundo,"1.5")),refeicao("Almoço",1));
		corpo.put("totais",Map.of("kcal",1));
		var criada=requisitar("POST","/pacientes/"+pid+"/dietas",a.token(),corpo);assertThat(criada.statusCode()).as(criada.body()).isEqualTo(201);
		var d=json(criada);assertThat(criada.headers().firstValue("ETag").orElseThrow()).isEqualTo("\"1\"");assertThat(d.path("status").asString()).isEqualTo("RASCUNHO");
		assertThat(d.path("pacienteId").asString()).isEqualTo(pid);assertThat(d.path("vinculoId").asString()).isEqualTo(p.path("vinculoId").asString());
		var kcalGrupo1=grupoKcal(a.token(),primeiro.path("grupoId").asString());var kcalGrupo2=grupoKcal(a.token(),segundo.path("grupoId").asString());
		var esperado=kcalGrupo1.multiply(new BigDecimal("2")).add(kcalGrupo2.multiply(new BigDecimal("1.5")));
		assertThat(d.path("totais").path("kcal").decimalValue()).isEqualByComparingTo(esperado);
		assertThat(d.path("refeicoes").get(0).path("totais").path("kcal").decimalValue()).isEqualByComparingTo(esperado);
		assertThat(d.path("refeicoes").get(1).path("itens")).isEmpty();assertThat(d.path("refeicoes").get(1).path("totais").path("kcal").decimalValue()).isEqualByComparingTo("0");
		assertThat(d.path("refeicoes").get(0).path("itens").get(0).path("nomeAlimento").asString()).isEqualTo(primeiro.path("nome").asString());
		assertThat(d.path("refeicoes").get(0).path("itens").get(0).path("grupoNome").asString()).isNotBlank();
		var lido=requisitar("GET","/dietas/"+d.path("id").asString(),a.token(),null);assertThat(lido.statusCode()).isEqualTo(200);assertThat(json(lido)).isEqualTo(d);
		var lista=json(requisitar("GET","/pacientes/"+pid+"/dietas",a.token(),null));assertThat(lista.path("itens")).hasSize(1);assertThat(lista.path("itens").get(0)).isEqualTo(d);
		assertThat(jdbc.queryForObject("select count(*) from log_acesso where usuario_id=? and entidade='dieta' and acao='ESCRITA'",Integer.class,a.usuario())).isEqualTo(1);
		assertThat(jdbc.queryForObject("select count(*) from log_acesso where usuario_id=? and entidade='dieta' and acao='LEITURA'",Integer.class,a.usuario())).isGreaterThanOrEqualTo(2);
	}
	@Test void putSubstituiDietaInteiraComIfMatchEConflitoDevolveAtual() throws Exception {
		var a=cadastrarNutri();var p=criarPaciente(a.token());String pid=p.path("id").asString();var ali=alimentos(a.token(),2);
		var d=criar(a.token(),pid,dieta("Original",refeicao("Café",0,item(ali.get(0),"1"))));String id=d.path("id").asString();
		var novo=dieta("Alterada",refeicao("Lanche",3,item(ali.get(1),"3")));
		var resposta=requisitar("PUT","/dietas/"+id,a.token(),novo,Map.of("If-Match","\"1\""));assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(200);
		var atualizada=json(resposta);assertThat(atualizada.path("versao").asLong()).isEqualTo(2);assertThat(atualizada.path("titulo").asString()).isEqualTo("Alterada");
		assertThat(atualizada.path("refeicoes")).hasSize(1);assertThat(atualizada.path("refeicoes").get(0).path("nome").asString()).isEqualTo("Lanche");
		assertThat(jdbc.queryForObject("select count(*) from refeicao where dieta_id=?",Integer.class,UUID.fromString(id))).isEqualTo(1);
		var conflito=requisitar("PUT","/dietas/"+id,a.token(),novo,Map.of("If-Match","\"1\""));erro(conflito,409,"CONFLITO_VERSAO");
		assertThat(json(conflito).path("atual").path("versao").asLong()).isEqualTo(2);
		erro(requisitar("PUT","/dietas/"+id,a.token(),novo,Map.of("If-Match","1")),400,"FORMATO_INVALIDO");
		var outroId=dieta("Id trocado");outroId.put("id",String.format("019a0000-0005-7000-8000-%012d",IDS.incrementAndGet()));
		erro(requisitar("PUT","/dietas/"+id,a.token(),outroId,Map.of("If-Match","\"2\"")),400,"FORMATO_INVALIDO");
	}
	@Test void idDeOutraDietaNaoEhReaproveitadoEIdempotenciaRepeteResposta() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var pa=criarPaciente(a.token());var pb=criarPaciente(b.token());var alia=alimentos(a.token(),1);var alib=alimentos(b.token(),1);
		String rid=String.format("019a0000-0005-7000-8000-%012d",IDS.incrementAndGet());
		var corpoA=dieta("A",refeicao("R",0,item(alia.get(0),"1")));((Map<String,Object>)((List<Object>)corpoA.get("refeicoes")).get(0)).put("id",rid);
		var da=requisitar("POST","/pacientes/"+pa.path("id").asString()+"/dietas",a.token(),corpoA);assertThat(da.statusCode()).isEqualTo(201);
		var corpoB=dieta("B",refeicao("R",0,item(alib.get(0),"1")));((Map<String,Object>)((List<Object>)corpoB.get("refeicoes")).get(0)).put("id",rid);
		var colisao=requisitar("POST","/pacientes/"+pb.path("id").asString()+"/dietas",b.token(),corpoB);assertThat(colisao.statusCode()).isEqualTo(409);
		assertThat(colisao.body()).doesNotContain("\"atual\":{");
		assertThat(jdbc.queryForObject("select count(*) from dieta where nutricionista_id=?",Integer.class,b.nutri())).isZero();
		String chave=UUID.randomUUID().toString();var corpo=dieta("Idempotente",refeicao("R",0,item(alia.get(0),"1")));
		var r1=requisitar("POST","/pacientes/"+pa.path("id").asString()+"/dietas",a.token(),corpo,Map.of("Idempotency-Key",chave));var r2=requisitar("POST","/pacientes/"+pa.path("id").asString()+"/dietas",a.token(),corpo,Map.of("Idempotency-Key",chave));
		assertThat(r1.statusCode()).isEqualTo(201);assertThat(r2.statusCode()).isEqualTo(201);assertThat(json(r2)).isEqualTo(json(r1));
		assertThat(jdbc.queryForObject("select count(*) from dieta where nutricionista_id=?",Integer.class,a.nutri())).isEqualTo(2);
	}
	@Test void nutriNaoEnxergaNemUsaDadosDeOutraNutri() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var pa=criarPaciente(a.token());String pid=pa.path("id").asString();var alia=alimentos(a.token(),1);var alib=alimentos(b.token(),1);
		var d=criar(a.token(),pid,dieta("Da A",refeicao("R",0,item(alia.get(0),"1"))));String id=d.path("id").asString();
		erro(requisitar("GET","/dietas/"+id,b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("GET","/pacientes/"+pid+"/dietas",b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("PUT","/dietas/"+id,b.token(),dieta("Invasão",refeicao("R",0,item(alib.get(0),"1"))),Map.of("If-Match","\"1\"")),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("POST","/pacientes/"+pid+"/dietas",b.token(),dieta("Invasão",refeicao("R",0,item(alib.get(0),"1")))),404,"RECURSO_NAO_ENCONTRADO");
		// alimento de outra nutri é indistinguível de inexistente
		erro(requisitar("POST","/pacientes/"+pid+"/dietas",a.token(),dieta("Alheio",refeicao("R",0,item(alib.get(0),"1")))),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("PUT","/dietas/"+id,a.token(),dieta("Alheio",refeicao("R",0,item(alib.get(0),"1"))),Map.of("If-Match","\"1\"")),404,"RECURSO_NAO_ENCONTRADO");
		assertThat(json(requisitar("GET","/dietas/"+id,a.token(),null)).path("titulo").asString()).isEqualTo("Da A");
		assertThat(requisitar("GET","/dietas/"+id,null,null).statusCode()).isEqualTo(401);
	}
	@Test void regrasDeEstadoVinculoAssinaturaEValidacao() throws Exception {
		var a=cadastrarNutri();var p=criarPaciente(a.token());String pid=p.path("id").asString();var ali=alimentos(a.token(),1);
		var d=criar(a.token(),pid,dieta("Rascunho",refeicao("R",0,item(ali.get(0),"1"))));String id=d.path("id").asString();
		jdbc.update("update alimento_nutri set ativo=false where id=?",UUID.fromString(ali.get(0).path("id").asString()));
		erro(requisitar("PUT","/dietas/"+id,a.token(),dieta("Inativo",refeicao("R",0,item(ali.get(0),"1"))),Map.of("If-Match","\"1\"")),400,"FORMATO_INVALIDO");
		jdbc.update("update alimento_nutri set ativo=true where id=?",UUID.fromString(ali.get(0).path("id").asString()));
		erro(requisitar("POST","/pacientes/"+pid+"/dietas",a.token(),dieta("Porções",refeicao("R",0,item(ali.get(0),"0")))),400,"FORMATO_INVALIDO");
		erro(requisitar("POST","/pacientes/"+pid+"/dietas",a.token(),dieta("Porções",refeicao("R",0,item(ali.get(0),"1.00001")))),400,"FORMATO_INVALIDO");
		var excesso=new ArrayList<Object>();for(int i=0;i<21;i++)excesso.add(refeicao("R"+i,i));
		var grande=dieta("Grande");grande.put("refeicoes",excesso);erro(requisitar("POST","/pacientes/"+pid+"/dietas",a.token(),grande),400,"FORMATO_INVALIDO");
		jdbc.update("update dieta set status='PUBLICADA' where id=?",UUID.fromString(id));
		erro(requisitar("PUT","/dietas/"+id,a.token(),dieta("Editar publicada",refeicao("R",0,item(ali.get(0),"1"))),Map.of("If-Match","\"1\"")),422,"DIETA_NAO_EDITAVEL");
		jdbc.update("update dieta set status='RASCUNHO' where id=?",UUID.fromString(id));
		jdbc.update("update assinatura set status='CANCELADA' where organizacao_id=?",a.organizacao());
		erro(requisitar("POST","/pacientes/"+pid+"/dietas",a.token(),dieta("Sem plano",refeicao("R",0,item(ali.get(0),"1")))),422,"ASSINATURA_INATIVA");
		erro(requisitar("PUT","/dietas/"+id,a.token(),dieta("Sem plano",refeicao("R",0,item(ali.get(0),"1"))),Map.of("If-Match","\"1\"")),422,"ASSINATURA_INATIVA");
		assertThat(requisitar("GET","/dietas/"+id,a.token(),null).statusCode()).isEqualTo(200);
		jdbc.update("update assinatura set status='TESTE' where organizacao_id=?",a.organizacao());
		assertThat(requisitar("POST","/pacientes/"+pid+"/encerrar-vinculo",a.token(),null).statusCode()).isEqualTo(200);
		erro(requisitar("PUT","/dietas/"+id,a.token(),dieta("Encerrado",refeicao("R",0,item(ali.get(0),"1"))),Map.of("If-Match","\"1\"")),403,"ACESSO_NEGADO");
		erro(requisitar("POST","/pacientes/"+pid+"/dietas",a.token(),dieta("Encerrado",refeicao("R",0,item(ali.get(0),"1")))),403,"ACESSO_NEGADO");
		assertThat(requisitar("GET","/dietas/"+id,a.token(),null).statusCode()).isEqualTo(200);
		assertThat(json(requisitar("GET","/pacientes/"+pid+"/dietas",a.token(),null)).path("itens")).hasSize(1);
	}
	JsonNode criarPaciente(String token) throws Exception {var r=requisitar("POST","/pacientes",token,Map.of("nome","Paciente fictício","email",UUID.randomUUID()+"@example.invalid"));assertThat(r.statusCode()).isEqualTo(201);return json(r);}
	List<JsonNode> alimentos(String token,int quantidade) throws Exception {
		var pagina=json(requisitar("GET","/alimentos?tamanho="+quantidade,token,null));var lista=new ArrayList<JsonNode>();pagina.path("itens").forEach(lista::add);return lista;
	}
	BigDecimal grupoKcal(String token,String grupo) throws Exception {
		for(var g:json(requisitar("GET","/grupos-alimentos",token,null)).path("itens"))if(g.path("id").asString().equals(grupo))return g.path("kcalPorPorcao").decimalValue();
		throw new AssertionError(grupo);
	}
	JsonNode criar(String token,String pid,Map<String,Object> corpo) throws Exception {var r=requisitar("POST","/pacientes/"+pid+"/dietas",token,corpo);assertThat(r.statusCode()).as(r.body()).isEqualTo(201);return json(r);}
	Map<String,Object> dieta(String titulo,Object... refeicoes){var m=new HashMap<String,Object>();m.put("titulo",titulo);m.put("refeicoes",new ArrayList<>(List.of(refeicoes)));return m;}
	Map<String,Object> refeicao(String nome,int ordem,Object... itens){var m=new HashMap<String,Object>();m.put("nome",nome);m.put("horario","08:00:00");m.put("ordem",ordem);m.put("itens",new ArrayList<>(List.of(itens)));return m;}
	Map<String,Object> item(JsonNode alimento,String porcoes){var m=new HashMap<String,Object>();m.put("alimentoId",alimento.path("id").asString());m.put("porcoes",new BigDecimal(porcoes));return m;}
}
