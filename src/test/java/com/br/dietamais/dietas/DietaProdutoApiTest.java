package com.br.dietamais.dietas;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

/** BE-48c: produto da nutri como item da dieta, `produtoId` + gramas (RN-ALI-05, RN-DIE-05): totais, micronutrientes, snapshot do paciente, cópia, modelo e isolamento. */
class DietaProdutoApiTest extends TesteComPaciente {
	Map<String,Object> produto(String nome){
		var n=new HashMap<String,Object>(Map.of("energia_kcal","380","carboidrato_g","8.5","proteina_g","78","lipideos_g","4.2","calcio_mg","100"));
		var m=new HashMap<String,Object>();m.put("nome",nome);m.put("medidaCaseira","colher(es) medida(s)");m.put("medidaGramas","30");m.put("nutrientes",n);return m;
	}
	String criarProduto(String token,String nome) throws Exception {
		var r=requisitar("POST","/produtos",token,produto(nome));assertThat(r.statusCode()).as(r.body()).isEqualTo(201);return json(r).path("id").asString();
	}
	Map<String,Object> itemProduto(String produtoId,String gramas){var m=new HashMap<String,Object>();m.put("produtoId",produtoId);if(gramas!=null)m.put("gramas",new BigDecimal(gramas));return m;}
	void desativar(String token,String produtoId,String nome) throws Exception {
		var edicao=produto(nome);edicao.put("ativo",false);
		assertThat(requisitar("PATCH","/produtos/"+produtoId,token,edicao,Map.of("If-Match","\"1\"")).statusCode()).isEqualTo(200);
	}

	@Test void produtoEntraNosTotaisComMicronutrientesEChegaAoPacienteNoSnapshot() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var ali=alimentos(a.token(),1);String prod=criarProduto(a.token(),"Whey da Ana");
		var d=criarDieta(a.token(),p.fichaId(),dieta("Com produto",refeicao("Lanche",0,item(ali.get(0),"1"),itemProduto(prod,"50"))));
		var itens=d.path("refeicoes").get(0).path("itens");var i=itens.get(1);
		assertThat(i.path("produtoId").asString()).isEqualTo(prod);assertThat(i.path("nomeAlimento").asString()).isEqualTo("Whey da Ana");
		assertThat(i.path("gramas").decimalValue()).isEqualByComparingTo("50");assertThat(i.path("porcoes").isNull()||i.path("porcoes").isMissingNode()).isTrue();
		var t=i.path("totais");
		assertThat(t.path("kcal").decimalValue()).isEqualByComparingTo("190.00");assertThat(t.path("carboidratos").decimalValue()).isEqualByComparingTo("4.25");
		assertThat(t.path("proteinas").decimalValue()).isEqualByComparingTo("39.00");assertThat(t.path("gorduras").decimalValue()).isEqualByComparingTo("2.10");
		assertThat(t.path("micronutrientes").path("ca").decimalValue()).isEqualByComparingTo("50");
		// o total da refeição soma o alimento e o produto
		var soma=itens.get(0).path("totais").path("kcal").decimalValue().add(t.path("kcal").decimalValue());
		assertThat(d.path("refeicoes").get(0).path("totais").path("kcal").decimalValue()).isEqualByComparingTo(soma);
		// o produto não informa ferro: o código entra em incompleto
		var incompleto=new ArrayList<String>();d.path("totais").path("incompleto").forEach(x->incompleto.add(x.asString()));assertThat(incompleto).contains("fe");
		// duplicar mantém o produto
		var copia=json(requisitar("POST","/dietas/"+d.path("id").asString()+"/duplicar",a.token(),null));
		assertThat(copia.path("refeicoes").get(0).path("itens").get(1).path("produtoId").asString()).isEqualTo(prod);
		// publicar congela o item e o paciente o recebe
		var snapshot=publicar(a.token(),d);
		assertThat(snapshot.path("refeicoes").get(0).path("itens").get(1).path("produtoId").asString()).isEqualTo(prod);
		var lista=json(requisitar("GET","/eu/dietas",p.token(),null)).path("itens");assertThat(lista).hasSize(1);
		assertThat(lista.get(0).path("refeicoes").get(0).path("itens").get(1).path("gramas").decimalValue()).isEqualByComparingTo("50");
		var pdf=requisitar("GET","/dietas/"+d.path("id").asString()+"/pdf",a.token(),null);assertThat(pdf.statusCode()).as(pdf.body()).isEqualTo(200);
	}
	@Test void rejeitaItemDeProdutoMalFormadoInativoOuDeOutraNutri() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var p=pacienteVinculado(a);var ali=alimentos(a.token(),1);String prod=criarProduto(a.token(),"Produto A");
		String alimento=ali.get(0).path("id").asString();
		// sem gramas; com porções; com alimento junto; à vontade; gramas em item de alimento
		erro(requisitar("POST","/pacientes/"+p.fichaId()+"/dietas",a.token(),dieta("T",refeicao("R",0,itemProduto(prod,null)))),422,"ITEM_INVALIDO");
		var comPorcoes=itemProduto(prod,"50");comPorcoes.put("porcoes",new BigDecimal("1"));
		erro(requisitar("POST","/pacientes/"+p.fichaId()+"/dietas",a.token(),dieta("T",refeicao("R",0,comPorcoes))),422,"ITEM_INVALIDO");
		var duasOrigens=itemProduto(prod,"50");duasOrigens.put("alimentoId",alimento);
		erro(requisitar("POST","/pacientes/"+p.fichaId()+"/dietas",a.token(),dieta("T",refeicao("R",0,duasOrigens))),422,"ITEM_INVALIDO");
		var aVontade=itemProduto(prod,"50");aVontade.put("aVontade",true);
		erro(requisitar("POST","/pacientes/"+p.fichaId()+"/dietas",a.token(),dieta("T",refeicao("R",0,aVontade))),422,"ITEM_INVALIDO");
		var gramasEmAlimento=new HashMap<String,Object>();gramasEmAlimento.put("alimentoId",alimento);gramasEmAlimento.put("porcoes",new BigDecimal("1"));gramasEmAlimento.put("gramas",new BigDecimal("10"));
		erro(requisitar("POST","/pacientes/"+p.fichaId()+"/dietas",a.token(),dieta("T",refeicao("R",0,gramasEmAlimento))),422,"ITEM_INVALIDO");
		// outra nutri não enxerga o produto (404); inativo não entra (RN-ALI-05)
		String deB=criarProduto(b.token(),"Produto B");
		erro(requisitar("POST","/pacientes/"+p.fichaId()+"/dietas",a.token(),dieta("T",refeicao("R",0,itemProduto(deB,"50")))),404,"RECURSO_NAO_ENCONTRADO");
		desativar(a.token(),prod,"Produto A");
		erro(requisitar("POST","/pacientes/"+p.fichaId()+"/dietas",a.token(),dieta("T",refeicao("R",0,itemProduto(prod,"50")))),400,"FORMATO_INVALIDO");
	}
	@Test void modeloDeDietaComProdutoMantemGramasEAvisaQuandoOProdutoSai() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);String prod=criarProduto(a.token(),"Whey modelo");
		var d=criarDieta(a.token(),p.fichaId(),dieta("Base",refeicao("Lanche",0,itemProduto(prod,"40"))));
		var salvo=requisitar("POST","/eu/nutri/modelos-dieta/de-dieta/"+d.path("id").asString(),a.token(),Map.of("titulo","Modelo com produto"));assertThat(salvo.statusCode()).as(salvo.body()).isEqualTo(201);
		var modelo=json(salvo);String modeloId=modelo.path("id").asString();var item=modelo.path("refeicoes").get(0).path("itens").get(0);
		assertThat(item.path("produtoId").asString()).isEqualTo(prod);assertThat(item.path("gramas").decimalValue()).isEqualByComparingTo("40");
		var aplicado=requisitar("POST","/pacientes/"+p.fichaId()+"/dietas/de-modelo/"+modeloId,a.token(),Map.of());assertThat(aplicado.statusCode()).as(aplicado.body()).isEqualTo(201);
		var novo=json(aplicado).path("dieta").path("refeicoes").get(0).path("itens").get(0);
		assertThat(novo.path("produtoId").asString()).isEqualTo(prod);assertThat(novo.path("gramas").decimalValue()).isEqualByComparingTo("40");
		// produto desativado: o item sai do rascunho, com aviso
		desativar(a.token(),prod,"Whey modelo");
		var sem=requisitar("POST","/pacientes/"+p.fichaId()+"/dietas/de-modelo/"+modeloId,a.token(),Map.of());assertThat(sem.statusCode()).as(sem.body()).isEqualTo(201);
		assertThat(json(sem).path("avisos").toString()).contains("ITEM_INDISPONIVEL");
	}
}
