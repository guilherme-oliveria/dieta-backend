package com.br.dietamais.dietas;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.TesteIsolamento;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** BE-52: modelos de dieta (RN-MOD-01 a RN-MOD-03): isolamento por nutri, sistema somente leitura, aplicar cria rascunho escalado. */
class ModelosDietaApiTest extends TesteIsolamento {
	static final AtomicLong IDS=new AtomicLong(9000);
	static String grupo(int n){return String.format("019a0000-0000-7000-8000-%012d",n);}
	static String novoId(){return String.format("019a0000-0009-7000-8000-%012d",IDS.incrementAndGet());}

	@Test void nutriCriaListaEditaEApagaSoOsDelaEOutraNutriNaoVe() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();
		var criado=requisitar("POST","/eu/nutri/modelos-dieta",a.token(),modelo("Hipertensão da Ana"));assertThat(criado.statusCode()).as(criado.body()).isEqualTo(201);
		var m=json(criado);String id=m.path("id").asString();assertThat(criado.headers().firstValue("ETag").orElseThrow()).isEqualTo("\"1\"");
		assertThat(m.path("escopo").asString()).isEqualTo("NUTRI");assertThat(m.path("restricoes").toString()).contains("ALTO_SODIO");
		assertThat(ids(json(requisitar("GET","/eu/nutri/modelos-dieta",a.token(),null)))).contains(id);
		assertThat(ids(json(requisitar("GET","/eu/nutri/modelos-dieta",b.token(),null)))).doesNotContain(id);
		// outra nutri: 404 em tudo (RN-MOD-01)
		erro(requisitar("GET","/eu/nutri/modelos-dieta/"+id,b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("PUT","/eu/nutri/modelos-dieta/"+id,b.token(),modelo("Roubado"),Map.of("If-Match","\"1\"")),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("DELETE","/eu/nutri/modelos-dieta/"+id,b.token(),null,Map.of("If-Match","\"1\"")),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("POST","/eu/nutri/modelos-dieta/"+id+"/duplicar",b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		String pb=paciente(b.token());
		erro(requisitar("POST","/pacientes/"+pb+"/dietas/de-modelo/"+id,b.token(),Map.of()),404,"RECURSO_NAO_ENCONTRADO");
		// editar com If-Match; versão velha é conflito com o atual
		var novo=modelo("Hipertensão editada");
		var editado=requisitar("PUT","/eu/nutri/modelos-dieta/"+id,a.token(),novo,Map.of("If-Match","\"1\""));assertThat(editado.statusCode()).as(editado.body()).isEqualTo(200);
		assertThat(json(editado).path("titulo").asString()).isEqualTo("Hipertensão editada");assertThat(json(editado).path("versao").asLong()).isEqualTo(2);
		erro(requisitar("PUT","/eu/nutri/modelos-dieta/"+id,a.token(),novo,Map.of("If-Match","\"1\"")),409,"CONFLITO_VERSAO");
		// duplicar vira outra cópia dela
		var copia=json(requisitar("POST","/eu/nutri/modelos-dieta/"+id+"/duplicar",a.token(),null));
		assertThat(copia.path("id").asString()).isNotEqualTo(id);assertThat(copia.path("titulo").asString()).endsWith("(cópia)");
		// apagar
		assertThat(requisitar("DELETE","/eu/nutri/modelos-dieta/"+id,a.token(),null,Map.of("If-Match","\"2\"")).statusCode()).isEqualTo(204);
		erro(requisitar("GET","/eu/nutri/modelos-dieta/"+id,a.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		assertThat(ids(json(requisitar("GET","/eu/nutri/modelos-dieta",a.token(),null)))).contains(copia.path("id").asString()).doesNotContain(id);
	}

	@Test void modeloDoSistemaEhSomenteLeituraMasTodaNutriCopiaEAplica() throws Exception {
		var a=cadastrarNutri();UUID sistema=UUID.fromString(novoId());
		jdbc.update("insert into modelo_dieta(id,nutricionista_id,titulo,refeicoes,metas,kcal_referencia) values(?,null,'Modelo fictício do sistema','[]'::jsonb,null,null)",sistema);
		try{
			var lista=json(requisitar("GET","/eu/nutri/modelos-dieta",a.token(),null)).path("itens");
			JsonNode meu=null;for(var x:lista)if(x.path("id").asString().equals(sistema.toString()))meu=x;
			assertThat(meu).isNotNull();assertThat(meu.path("escopo").asString()).isEqualTo("SISTEMA");
			erro(requisitar("PUT","/eu/nutri/modelos-dieta/"+sistema,a.token(),modelo("Alterado"),Map.of("If-Match","\"1\"")),403,"ACESSO_NEGADO");
			erro(requisitar("DELETE","/eu/nutri/modelos-dieta/"+sistema,a.token(),null,Map.of("If-Match","\"1\"")),403,"ACESSO_NEGADO");
			var copia=requisitar("POST","/eu/nutri/modelos-dieta/"+sistema+"/duplicar",a.token(),null);assertThat(copia.statusCode()).as(copia.body()).isEqualTo(201);
			assertThat(json(copia).path("escopo").asString()).isEqualTo("NUTRI");
		}finally{jdbc.update("delete from modelo_dieta where id=?",sistema);}
	}

	@Test void aplicarCriaRascunhoEscaladoPeloAlvoSemPublicar() throws Exception {
		var a=cadastrarNutri();String pid=paciente(a.token());
		var criado=json(requisitar("POST","/eu/nutri/modelos-dieta",a.token(),modelo("Escala")));String id=criado.path("id").asString();
		var r=requisitar("POST","/pacientes/"+pid+"/dietas/de-modelo/"+id,a.token(),Map.of("vetKcal",2000));assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
		var res=json(r);var d=res.path("dieta");
		assertThat(res.path("fatorEscala").decimalValue()).isEqualByComparingTo("2");assertThat(d.path("status").asString()).isEqualTo("RASCUNHO");
		assertThat(d.path("metas").path("vetKcal").decimalValue()).isEqualByComparingTo("2000");assertThat(d.path("metas").path("carboidratoPct").decimalValue()).isEqualByComparingTo("50");
		var itens=d.path("refeicoes").get(0).path("itens");
		assertThat(itens.get(0).path("porcoes").decimalValue()).isEqualByComparingTo("4");assertThat(itens.get(1).path("porcoes").decimalValue()).isEqualByComparingTo("2");
		assertThat(itens.get(2).path("aVontade").asBoolean()).isTrue();
		assertThat(d.path("pacienteId").asString()).isEqualTo(pid);assertThat(res.path("avisos")).isEmpty();
		assertThat(jdbc.queryForObject("select count(*) from dieta_snapshot where dieta_id=?",Integer.class,UUID.fromString(d.path("id").asString()))).isZero();
		assertThat(json(requisitar("GET","/dietas/"+d.path("id").asString(),a.token(),null)).path("status").asString()).isEqualTo("RASCUNHO");
		// sem alvo: porções do modelo; alvo absurdo: 400
		var igual=json(requisitar("POST","/pacientes/"+pid+"/dietas/de-modelo/"+id,a.token(),Map.of()));
		assertThat(igual.path("fatorEscala").decimalValue()).isEqualByComparingTo("1");assertThat(igual.path("dieta").path("refeicoes").get(0).path("itens").get(0).path("porcoes").decimalValue()).isEqualByComparingTo("2");
		erro(requisitar("POST","/pacientes/"+pid+"/dietas/de-modelo/"+id,a.token(),Map.of("vetKcal",19000)),400,"FORMATO_INVALIDO");
	}

	@Test void restricaoDoModeloTiraAlimentoMarcadoEAvisa() throws Exception {
		var a=cadastrarNutri();String pid=paciente(a.token());
		String g=json(requisitar("GET","/grupos-alimentos",a.token(),null)).path("itens").get(0).path("id").asString();
		var salgado=json(requisitar("POST","/alimentos",a.token(),alimento(g,"Salgado fictício",List.of("ALTO_SODIO"))));
		var doce=json(requisitar("POST","/alimentos",a.token(),alimento(g,"Neutro fictício",List.of())));
		var corpo=modelo("Com restrição");var refeicao=(Map<String,Object>)((List<?>)corpo.get("refeicoes")).get(0);
		var itens=new ArrayList<Object>((List<?>)refeicao.get("itens"));
		itens.add(Map.of("alimentoId",salgado.path("id").asString(),"porcoes",new BigDecimal("1")));itens.add(Map.of("alimentoId",doce.path("id").asString(),"porcoes",new BigDecimal("1")));
		refeicao.put("itens",itens);
		var m=requisitar("POST","/eu/nutri/modelos-dieta",a.token(),corpo);assertThat(m.statusCode()).as(m.body()).isEqualTo(201);
		var res=json(requisitar("POST","/pacientes/"+pid+"/dietas/de-modelo/"+json(m).path("id").asString(),a.token(),Map.of()));
		var nomes=new ArrayList<String>();for(var i:res.path("dieta").path("refeicoes").get(0).path("itens"))nomes.add(i.path("nomeAlimento").asString());
		assertThat(nomes).contains("Neutro fictício").doesNotContain("Salgado fictício");
		assertThat(res.path("avisos")).hasSize(1);assertThat(res.path("avisos").get(0).path("codigo").asString()).isEqualTo("ITEM_REMOVIDO_RESTRICAO");
	}

	@Test void salvarDietaComoModeloGuardaConteudoEEnergiaDeReferencia() throws Exception {
		var a=cadastrarNutri();var outra=cadastrarNutri();String pid=paciente(a.token());
		var modelo=json(requisitar("POST","/eu/nutri/modelos-dieta",a.token(),modelo("Origem")));
		var dieta=json(requisitar("POST","/pacientes/"+pid+"/dietas/de-modelo/"+modelo.path("id").asString(),a.token(),Map.of())).path("dieta");
		var r=requisitar("POST","/eu/nutri/modelos-dieta/de-dieta/"+dieta.path("id").asString(),a.token(),Map.of("titulo","Salvo da dieta","restricoes",List.of("CONTEM_LACTOSE")));
		assertThat(r.statusCode()).as(r.body()).isEqualTo(201);var salvo=json(r);
		assertThat(salvo.path("kcalReferencia").decimalValue()).isEqualByComparingTo(dieta.path("totais").path("kcal").decimalValue());
		assertThat(salvo.path("refeicoes")).hasSize(1);assertThat(salvo.path("restricoes").toString()).contains("CONTEM_LACTOSE");
		// dieta de outra nutri: 404
		erro(requisitar("POST","/eu/nutri/modelos-dieta/de-dieta/"+dieta.path("id").asString(),outra.token(),Map.of("titulo","Roubada")),404,"RECURSO_NAO_ENCONTRADO");
	}

	@Test void conteudoInvalidoEhRecusadoComAsRegrasDaDieta() throws Exception {
		var a=cadastrarNutri();
		var corpo=modelo("Inválido");((List<Object>)corpo.get("porcoes")).clear();
		erro(requisitar("POST","/eu/nutri/modelos-dieta",a.token(),corpo),422,"PORCAO_INVALIDA");
	}

	/** BE-52b, RN-MOD-03: a restrição do modelo vai para a dieta, sobrevive à edição sem o campo e tira da lista do paciente na publicação. */
	@Test void restricaoDoModeloVaiParaADietaEFiltraAListaDoSnapshot() throws Exception {
		var a=cadastrarNutri();
		String g=json(requisitar("GET","/grupos-alimentos",a.token(),null)).path("itens").get(0).path("id").asString();
		var salgado=json(requisitar("POST","/alimentos",a.token(),alimento(g,"Salgado fictício",List.of("ALTO_SODIO"))));
		var neutro=json(requisitar("POST","/alimentos",a.token(),alimento(g,"Neutro fictício",List.of())));
		// a lista do paciente nasce com os alimentos que a nutri já tem: criar o paciente depois
		String pid=paciente(a.token());
		var comRestricao=json(requisitar("POST","/eu/nutri/modelos-dieta",a.token(),modelo("Com restrição")));
		var d=json(requisitar("POST","/pacientes/"+pid+"/dietas/de-modelo/"+comRestricao.path("id").asString(),a.token(),Map.of())).path("dieta");
		assertThat(d.path("restricoes").toString()).contains("ALTO_SODIO");
		// editar sem informar restricoes mantém; informar vazio limpa
		var corpo=new HashMap<String,Object>();corpo.put("titulo","Editada");
		var refeicao=modelo("x").get("refeicoes");corpo.put("refeicoes",refeicao);corpo.put("porcoes",modelo("x").get("porcoes"));
		var put=requisitar("PUT","/dietas/"+d.path("id").asString(),a.token(),corpo,Map.of("If-Match","\"1\""));assertThat(put.statusCode()).as(put.body()).isEqualTo(200);
		assertThat(json(put).path("restricoes").toString()).contains("ALTO_SODIO");
		var snap=json(requisitar("POST","/dietas/"+d.path("id").asString()+"/publicar",a.token(),null,Map.of("If-Match","\"2\"")));
		assertThat(snap.path("restricoes").toString()).contains("ALTO_SODIO");
		var naLista=new ArrayList<String>();snap.path("listaSubstituicao").path("alimentos").forEach(x->naLista.add(x.path("id").asString()));
		assertThat(naLista).contains(neutro.path("id").asString()).doesNotContain(salgado.path("id").asString());
		// sem restrição, o alimento marcado continua na lista
		var livre=modelo("Livre");livre.put("restricoes",List.of());
		var m2=json(requisitar("POST","/eu/nutri/modelos-dieta",a.token(),livre));
		var d2=json(requisitar("POST","/pacientes/"+pid+"/dietas/de-modelo/"+m2.path("id").asString(),a.token(),Map.of())).path("dieta");
		assertThat(d2.has("restricoes")).isFalse();
		var snap2=json(requisitar("POST","/dietas/"+d2.path("id").asString()+"/publicar",a.token(),null,Map.of("If-Match","\"1\"")));
		var lista2=new ArrayList<String>();snap2.path("listaSubstituicao").path("alimentos").forEach(x->lista2.add(x.path("id").asString()));
		assertThat(lista2).contains(salgado.path("id").asString());
	}

	/** BE-53: nutri nova já vê os modelos iniciais do sistema e todos se aplicam, escalados, como rascunho. */
	@Test void modelosIniciaisDoSistemaAparecemParaNutriNovaEAplicamSemErro() throws Exception {
		var a=cadastrarNutri();String pid=paciente(a.token());
		var itens=json(requisitar("GET","/eu/nutri/modelos-dieta",a.token(),null)).path("itens");
		var sistema=new ArrayList<JsonNode>();for(var x:itens)if("SISTEMA".equals(x.path("escopo").asString()))sistema.add(x);
		assertThat(sistema).hasSizeGreaterThanOrEqualTo(9);
		var titulos=new ArrayList<String>();sistema.forEach(x->titulos.add(x.path("titulo").asString()));
		assertThat(titulos).contains("Sem restrições","Hipertensão (DASH)","Intolerância à lactose","Diabetes tipo 2","Dislipidemia","Doença celíaca","Gestante","Obesidade (hipocalórica)","Doença renal crônica (sem diálise)");
		for(var m:sistema){
			var r=requisitar("POST","/pacientes/"+pid+"/dietas/de-modelo/"+m.path("id").asString(),a.token(),Map.of("vetKcal",1800));
			assertThat(r.statusCode()).as(m.path("titulo").asString()+": "+r.body()).isEqualTo(201);
			var d=json(r).path("dieta");assertThat(d.path("status").asString()).isEqualTo("RASCUNHO");
			assertThat(d.path("totais").path("kcal").decimalValue()).isBetween(new BigDecimal("1000"),new BigDecimal("2700"));
		}
		var dash=sistema.stream().filter(x->x.path("titulo").asString().startsWith("Hipertensão")).findFirst().orElseThrow();
		assertThat(dash.path("restricoes").toString()).contains("ALTO_SODIO");
	}

	// ---- auxiliares
	String paciente(String token) throws Exception {var r=requisitar("POST","/pacientes",token,Map.of("nome","Paciente fictício","email",UUID.randomUUID()+"@example.invalid"));assertThat(r.statusCode()).isEqualTo(201);return json(r).path("id").asString();}
	List<String> ids(JsonNode lista){var l=new ArrayList<String>();lista.path("itens").forEach(x->l.add(x.path("id").asString()));return l;}
	Map<String,Object> alimento(String grupo,String nome,List<String> marcadores){return Map.of("id",novoId(),"grupoId",grupo,"nome",nome,"medidaCaseira","1 unidade fictícia","gramasPorPorcao",new BigDecimal("10"),"marcadores",marcadores);}
	/** Café com 2 porções do grupo 1, 1 do grupo 6 e um item à vontade; metas de 1000 kcal como referência. */
	Map<String,Object> modelo(String titulo){
		var m=new HashMap<String,Object>();m.put("titulo",titulo);m.put("condicao","Hipertensão");m.put("kcalReferencia",new BigDecimal("1000"));m.put("restricoes",List.of("ALTO_SODIO"));
		m.put("porcoes",new ArrayList<Object>(List.of(Map.of("grupoId",grupo(1),"opcao",1),Map.of("grupoId",grupo(6),"opcao",1))));
		m.put("metas",Map.of("vetKcal",1000,"carboidratoPct",50,"proteinaPct",20,"lipidioPct",30));
		var r=new HashMap<String,Object>();r.put("id",novoId());r.put("nome","Café");r.put("horario","08:00:00");r.put("ordem",0);
		var vontade=new HashMap<String,Object>();vontade.put("grupoId",grupo(13));vontade.put("aVontade",true);
		r.put("itens",new ArrayList<Object>(List.of(Map.of("grupoId",grupo(1),"porcoes",new BigDecimal("2")),Map.of("grupoId",grupo(6),"porcoes",new BigDecimal("1")),vontade)));
		m.put("refeicoes",new ArrayList<Object>(List.of(r)));return m;
	}
}
