package com.br.dietamais.dietas;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.TesteIsolamento;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** BE-49a: dieta por porções do grupo, metas, alternativas, à vontade e rodapé (RN-DIE-10 a RN-DIE-12, RN-CAL-07). */
class DietaPorcoesApiTest extends TesteIsolamento {
	static final AtomicLong IDS=new AtomicLong(7000);
	static String grupo(int n){return String.format("019a0000-0000-7000-8000-%012d",n);}
	static String novoId(){return String.format("019a0000-0007-7000-8000-%012d",IDS.incrementAndGet());}

	@Test void salvaPorPorcoesCalculaTotaisSaldoEAlternativaForaDoDia() throws Exception {
		var a=cadastrarNutri();String pid=paciente(a.token());
		String principal=novoId();
		var desjejum=refeicao("Desjejum","PRINCIPAL",principal,null,porGrupo(1,"2"),porGrupo(6,"1"),porGrupo(7,"1"),aVontade(13),textoLivre("Café sem açúcar"));
		var alternativa=refeicao("Desjejum 2ª opção","ALTERNATIVA",novoId(),principal,porGrupo(9,"1"));
		var corpo=dieta("Por porções",desjejum,alternativa);
		corpo.put("porcoes",List.of(escolha(1,1),escolha(6,2),escolha(7,1),escolha(9,1)));
		corpo.put("metas",Map.of("vetKcal",1800,"carboidratoPct",50,"proteinaPct",20,"lipidioPct",30));
		corpo.put("rodape","Além das refeições acima, você também poderá ingerir verduras à vontade.");
		var r=requisitar("POST","/pacientes/"+pid+"/dietas",a.token(),corpo);assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
		var d=json(r);
		var m=d.path("refeicoes").get(0);
		// vetor "Desjejum" de dieta-porcoes.json
		assertThat(m.path("totais").path("kcal").decimalValue()).isCloseTo(new BigDecimal("279.63"),within(new BigDecimal("0.01")));
		assertThat(m.path("totais").path("carboidratos").decimalValue()).isCloseTo(new BigDecimal("47.34"),within(new BigDecimal("0.01")));
		assertThat(m.path("totais").path("proteinas").decimalValue()).isCloseTo(new BigDecimal("13.63"),within(new BigDecimal("0.01")));
		assertThat(m.path("totais").path("gorduras").decimalValue()).isCloseTo(new BigDecimal("6.55"),within(new BigDecimal("0.01")));
		var itens=m.path("itens");assertThat(itens).hasSize(5);
		assertThat(itens.get(0).path("grupoId").asString()).isEqualTo(grupo(1));assertThat(itens.get(0).path("alimentoId").isMissingNode()||itens.get(0).path("alimentoId").isNull()).isTrue();
		assertThat(itens.get(0).path("opcao").asInt()).isEqualTo(1);assertThat(itens.get(0).path("medidaCaseira").asString()).isNotBlank();
		assertThat(itens.get(3).path("aVontade").asBoolean()).isTrue();assertThat(itens.get(3).path("totais").path("kcal").decimalValue()).isEqualByComparingTo("0");
		assertThat(itens.get(4).path("textoLivre").asString()).isEqualTo("Café sem açúcar");assertThat(itens.get(4).path("totais").path("kcal").decimalValue()).isEqualByComparingTo("0");
		// RN-DIE-11: a alternativa não entra no dia e mostra a diferença para a principal
		var alt=d.path("refeicoes").get(1);assertThat(alt.path("tipo").asString()).isEqualTo("ALTERNATIVA");assertThat(alt.path("alternativaDe").asString()).isEqualTo(principal);
		assertThat(alt.path("totais").path("kcal").decimalValue()).isEqualByComparingTo("29.61");
		assertThat(alt.path("diferencaParaPrincipal").path("kcal").decimalValue()).isEqualByComparingTo(alt.path("totais").path("kcal").decimalValue().subtract(m.path("totais").path("kcal").decimalValue()));
		assertThat(d.path("totais").path("kcal").decimalValue()).isEqualByComparingTo(m.path("totais").path("kcal").decimalValue());
		// saldo das metas: VET 1800; 50% carbo e 20% proteína a 4 kcal/g; 30% lipídio a 9 kcal/g
		var saldo=d.path("totais").path("saldoMetas");
		assertThat(saldo.path("kcal").decimalValue()).isEqualByComparingTo(new BigDecimal("1800").subtract(d.path("totais").path("kcal").decimalValue()));
		assertThat(saldo.path("carboidrato").decimalValue()).isEqualByComparingTo(new BigDecimal("225").subtract(d.path("totais").path("carboidratos").decimalValue()));
		assertThat(saldo.path("proteina").decimalValue()).isEqualByComparingTo(new BigDecimal("90").subtract(d.path("totais").path("proteinas").decimalValue()));
		assertThat(saldo.path("lipidio").decimalValue()).isEqualByComparingTo(new BigDecimal("60").subtract(d.path("totais").path("gorduras").decimalValue()));
		assertThat(d.path("rodape").asString()).startsWith("Além das refeições");assertThat(d.path("porcoes")).hasSize(4);assertThat(d.path("metas").path("vetKcal").decimalValue()).isEqualByComparingTo("1800");
		assertThat(json(requisitar("GET","/dietas/"+d.path("id").asString(),a.token(),null))).isEqualTo(d);
		// publicar congela porções, metas e rodapé (RN-DIE-03)
		var snapshot=json(requisitar("POST","/dietas/"+d.path("id").asString()+"/publicar",a.token(),null,Map.of("If-Match","\"1\"")));
		assertThat(snapshot.path("rodape").asString()).startsWith("Além das refeições");assertThat(snapshot.path("porcoes")).hasSize(4);assertThat(snapshot.path("metas").path("carboidratoPct").decimalValue()).isEqualByComparingTo("50");
		assertThat(snapshot.path("totais").path("kcal").decimalValue()).isEqualByComparingTo(d.path("totais").path("kcal").decimalValue());
		// duplicar copia tudo, inclusive a ligação da alternativa com a nova principal
		var copia=json(requisitar("POST","/dietas/"+d.path("id").asString()+"/duplicar",a.token(),null));
		assertThat(copia.path("refeicoes").get(1).path("alternativaDe").asString()).isEqualTo(copia.path("refeicoes").get(0).path("id").asString()).isNotEqualTo(principal);
		assertThat(copia.path("rodape").asString()).isEqualTo(d.path("rodape").asString());assertThat(copia.path("porcoes")).hasSize(4);
	}
	@Test void rejeitaItemSemOrigemUnicaEPorcaoInvalida() throws Exception {
		var a=cadastrarNutri();String pid=paciente(a.token());var ali=json(requisitar("GET","/alimentos?tamanho=1",a.token(),null)).path("itens").get(0);
		var escolha=List.of(escolha(1,1));
		// sem origem
		erro(salvar(a.token(),pid,escolha,itemSolto(null,null,"1",false,null)),422,"ITEM_INVALIDO");
		// duas origens
		erro(salvar(a.token(),pid,escolha,itemSolto(ali.path("id").asString(),grupo(1),"1",false,null)),422,"ITEM_INVALIDO");
		// sem quantidade e sem à vontade
		erro(salvar(a.token(),pid,escolha,itemSolto(null,grupo(1),null,false,null)),422,"ITEM_INVALIDO");
		// à vontade com quantidade; texto livre junto de uma origem
		erro(salvar(a.token(),pid,escolha,itemSolto(null,grupo(13),"1",true,null)),422,"ITEM_INVALIDO");
		erro(salvar(a.token(),pid,escolha,itemSolto(null,grupo(1),"1",false,"Pão")),422,"ITEM_INVALIDO");
		// grupo sem a porção escolhida, grupo inexistente, opção inexistente
		erro(salvar(a.token(),pid,escolha,itemSolto(null,grupo(6),"1",false,null)),422,"PORCAO_INVALIDA");
		erro(salvar(a.token(),pid,escolha,itemSolto(null,UUID.randomUUID().toString(),null,true,null)),422,"PORCAO_INVALIDA");
		erro(salvar(a.token(),pid,List.of(escolha(1,9)),itemSolto(null,grupo(1),"1",false,null)),422,"PORCAO_INVALIDA");
		erro(salvar(a.token(),pid,List.of(escolha(1,1),escolha(1,2)),itemSolto(null,grupo(1),"1",false,null)),422,"PORCAO_INVALIDA");
		// alternativa sem refeição principal desta dieta
		var corpo=dieta("Alternativa solta",refeicao("Lanche","ALTERNATIVA",novoId(),novoId(),porGrupo(1,"1")));corpo.put("porcoes",escolha);
		erro(requisitar("POST","/pacientes/"+pid+"/dietas",a.token(),corpo),400,"FORMATO_INVALIDO");
		assertThat(jdbc.queryForObject("select count(*) from dieta where nutricionista_id=?",Integer.class,a.nutri())).isZero();
	}
	@Test void dietaPorPorcoesDeUmaNutriNaoApareceParaOutra() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();String pid=paciente(a.token());
		var corpo=dieta("Da A",refeicao("R","PRINCIPAL",novoId(),null,porGrupo(1,"1")));corpo.put("porcoes",List.of(escolha(1,1)));
		var d=json(requisitar("POST","/pacientes/"+pid+"/dietas",a.token(),corpo));
		erro(requisitar("GET","/dietas/"+d.path("id").asString(),b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("PUT","/dietas/"+d.path("id").asString(),b.token(),corpo,Map.of("If-Match","\"1\"")),404,"RECURSO_NAO_ENCONTRADO");
	}

	BigDecimal itensKcal(JsonNode refeicao){return refeicao.path("totais").path("kcal").decimalValue();}
	String paciente(String token) throws Exception {var r=requisitar("POST","/pacientes",token,Map.of("nome","Paciente fictício","email",UUID.randomUUID()+"@example.invalid"));assertThat(r.statusCode()).isEqualTo(201);return json(r).path("id").asString();}
	java.net.http.HttpResponse<String> salvar(String token,String pid,List<Map<String,Object>> porcoes,Map<String,Object> item) throws Exception {
		var corpo=dieta("Teste",refeicao("R","PRINCIPAL",novoId(),null,item));corpo.put("porcoes",porcoes);return requisitar("POST","/pacientes/"+pid+"/dietas",token,corpo);
	}
	Map<String,Object> escolha(int grupo,int opcao){return Map.of("grupoId",grupo(grupo),"opcao",opcao);}
	Map<String,Object> dieta(String titulo,Object... refeicoes){var m=new HashMap<String,Object>();m.put("titulo",titulo);m.put("refeicoes",new ArrayList<>(List.of(refeicoes)));return m;}
	Map<String,Object> refeicao(String nome,String tipo,String id,String alternativaDe,Object... itens){
		var m=new HashMap<String,Object>();m.put("id",id);m.put("nome",nome);m.put("horario","08:00:00");m.put("ordem",0);m.put("tipo",tipo);if(alternativaDe!=null)m.put("alternativaDe",alternativaDe);m.put("itens",new ArrayList<>(List.of(itens)));return m;
	}
	Map<String,Object> porGrupo(int grupo,String porcoes){return itemSolto(null,grupo(grupo),porcoes,false,null);}
	Map<String,Object> aVontade(int grupo){return itemSolto(null,grupo(grupo),null,true,null);}
	Map<String,Object> textoLivre(String texto){return itemSolto(null,null,null,true,texto);}
	Map<String,Object> itemSolto(String alimentoId,String grupoId,String porcoes,boolean aVontade,String texto){
		var m=new HashMap<String,Object>();if(alimentoId!=null)m.put("alimentoId",alimentoId);if(grupoId!=null)m.put("grupoId",grupoId);if(porcoes!=null)m.put("porcoes",new BigDecimal(porcoes));
		m.put("aVontade",aVontade);if(texto!=null)m.put("textoLivre",texto);return m;
	}
}
