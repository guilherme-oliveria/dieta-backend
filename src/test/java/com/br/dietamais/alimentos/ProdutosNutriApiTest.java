package com.br.dietamais.alimentos;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.TesteIsolamento;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** BE-48b: produtos e suplementos da nutri (RN-ALI-05): cria, edita, desativa; outra nutri não vê nada (404); entram no sync e se escrevem pelo {@code POST /sync}. */
class ProdutosNutriApiTest extends TesteIsolamento {
	static Map<String,Object> nutrientes(){return new HashMap<>(Map.of("energia_kcal","380","carboidrato_g","8.5","proteina_g","78","lipideos_g","4.2","sodio_mg","210"));}
	static Map<String,Object> produto(String nome){
		var m=new HashMap<String,Object>();m.put("nome",nome);m.put("marca","Marca da Ana");m.put("classificacao","alimento proteico PPAF");m.put("apresentacao","pó");
		m.put("medidaCaseira","colher(es) medida(s)");m.put("medidaGramas","30");m.put("observacao","Sem glúten.");m.put("nutrientes",nutrientes());return m;
	}
	Map<String,Object> edicao(String nome,boolean ativo){var m=produto(nome);m.put("ativo",ativo);return m;}
	static final int CATALOGO=400; // BE-48d: toda nutri nasce com os 400 produtos do catálogo base
	String id(){return jdbc.queryForObject("select uuidv7()",String.class);}
	Map<String,String> ifMatch(long v){return Map.of("If-Match","\""+v+"\"");}

	@Test void nutriCadastraEditaEDesativaEOutraNutriNaoVeNada() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();
		var criado=requisitar("POST","/produtos",a.token(),produto("Whey Zebrafit"));assertThat(criado.statusCode()).as(criado.body()).isEqualTo(201);
		var p=json(criado);String id=p.path("id").asString();assertThat(criado.headers().firstValue("ETag").orElseThrow()).isEqualTo("\"1\"");
		assertThat(p.path("ativo").asBoolean()).isTrue();assertThat(p.path("nutrientes").path("energia_kcal").decimalValue()).isEqualByComparingTo("380");
		assertThat(p.path("medidaGramas").decimalValue()).isEqualByComparingTo("30");
		assertThat(json(requisitar("GET","/produtos?busca=zebrafit%20whey",a.token(),null)).path("total").asLong()).isEqualTo(1);
		assertThat(json(requisitar("GET","/produtos?busca=zebrafit",b.token(),null)).path("total").asLong()).isZero();
		// outra nutri: 404 em tudo (RN-ALI-05), sem revelar que existe
		erro(requisitar("GET","/produtos/"+id,b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("PATCH","/produtos/"+id,b.token(),edicao("Roubado",true),ifMatch(1)),404,"RECURSO_NAO_ENCONTRADO");
		// editar com If-Match; versão velha é conflito com o atual
		var editado=requisitar("PATCH","/produtos/"+id,a.token(),edicao("Whey editado",true),ifMatch(1));assertThat(editado.statusCode()).as(editado.body()).isEqualTo(200);
		assertThat(json(editado).path("nome").asString()).isEqualTo("Whey editado");assertThat(json(editado).path("versao").asLong()).isEqualTo(2);
		var conflito=requisitar("PATCH","/produtos/"+id,a.token(),edicao("Outro",true),ifMatch(1));erro(conflito,409,"CONFLITO_VERSAO");
		// desativar não apaga: some da lista ativa e aparece com ativo=false
		var desativado=requisitar("PATCH","/produtos/"+id,a.token(),edicao("Whey editado",false),ifMatch(2));assertThat(desativado.statusCode()).isEqualTo(200);
		assertThat(json(requisitar("GET","/produtos",a.token(),null)).path("total").asLong()).isEqualTo(CATALOGO);
		var inativos=json(requisitar("GET","/produtos?ativo=false",a.token(),null));assertThat(inativos.path("total").asLong()).isEqualTo(1);
		assertThat(inativos.path("itens").get(0).path("id").asString()).isEqualTo(id);
		assertThat(json(requisitar("GET","/produtos/"+id,a.token(),null)).path("ativo").asBoolean()).isFalse();
	}

	@Test void validaNutrientesEIdUuidV7() throws Exception {
		var a=cadastrarNutri();
		var semMacro=produto("Sem proteína");((Map<String,Object>)semMacro.get("nutrientes")).remove("proteina_g");
		erro(requisitar("POST","/produtos",a.token(),semMacro),400,"OBRIGATORIO");
		var chaveInvalida=produto("Chave estranha");((Map<String,Object>)chaveInvalida.get("nutrientes")).put("nutriente_inventado","1");
		erro(requisitar("POST","/produtos",a.token(),chaveInvalida),400,"FORMATO_INVALIDO");
		var negativo=produto("Negativo");((Map<String,Object>)negativo.get("nutrientes")).put("sodio_mg","-1");
		erro(requisitar("POST","/produtos",a.token(),negativo),400,"FORMATO_INVALIDO");
		var semNome=produto(" ");
		assertThat(requisitar("POST","/produtos",a.token(),semNome).statusCode()).isBetween(400,422);
		var v4=produto("Id v4");v4.put("id",UUID.randomUUID().toString());
		erro(requisitar("POST","/produtos",a.token(),v4),400,"FORMATO_INVALIDO");
		var comId=produto("Com id do app");String id=id();comId.put("id",id);
		assertThat(json(requisitar("POST","/produtos",a.token(),comId)).path("id").asString()).isEqualTo(id);
		assertThat(json(requisitar("GET","/produtos",a.token(),null)).path("total").asLong()).isEqualTo(CATALOGO+1);
	}

	@Test void produtoEntraNoSyncSoDaNutriDonaEEscreveEntrePeloSync() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();String id=id();
		var mut=new HashMap<String,Object>();mut.put("idMutacao",UUID.randomUUID().toString());mut.put("entidade","produtos");mut.put("operacao","CRIAR");mut.put("id",id);mut.put("versaoBase",null);mut.put("dados",produto("Criado offline"));
		var r=requisitar("POST","/sync",a.token(),Map.of("mutacoes",List.of(mut)));assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
		assertThat(json(r).path("resultados").get(0).path("status").asString()).as(r.body()).isEqualTo("APLICADA");
		var edit=new HashMap<String,Object>();edit.put("idMutacao",UUID.randomUUID().toString());edit.put("entidade","produtos");edit.put("operacao","ATUALIZAR");edit.put("id",id);edit.put("versaoBase",1);edit.put("dados",edicao("Editado offline",true));
		var r2=requisitar("POST","/sync",a.token(),Map.of("mutacoes",List.of(edit)));assertThat(json(r2).path("resultados").get(0).path("status").asString()).as(r2.body()).isEqualTo("APLICADA");
		var pa=json(requisitar("GET","/sync?limite=1000",a.token(),null)).path("mudancas").path("produtos");
		// BE-48d: as 400 cópias do catálogo não entram no sync ao nascer (a lista é online); só o que a nutri cria ou edita
		assertThat(pa).hasSize(1);assertThat(pa.get(0).path("id").asString()).isEqualTo(id);assertThat(pa.get(0).path("dados").path("nome").asString()).isEqualTo("Editado offline");
		var pb=json(requisitar("GET","/sync?limite=1000",b.token(),null)).path("mudancas").path("produtos");
		assertThat(pb.isMissingNode()?0:pb.size()).isZero();
		// a outra nutri não consegue escrever no produto alheio pelo sync
		var roubo=new HashMap<String,Object>(edit);roubo.put("idMutacao",UUID.randomUUID().toString());roubo.put("dados",edicao("Roubado",true));
		var r3=requisitar("POST","/sync",b.token(),Map.of("mutacoes",List.of(roubo)));assertThat(json(r3).path("resultados").get(0).path("status").asString()).isNotEqualTo("APLICADA");
		assertThat(json(requisitar("GET","/produtos/"+id,a.token(),null)).path("nome").asString()).isEqualTo("Editado offline");
	}

	@Test void nutriNovaNasceComOsProdutosDoCatalogoEditaveisECadaUmaTemOsSeus() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();
		var lista=json(requisitar("GET","/produtos?tamanho=100&busca=100%25%20pure%20whey",a.token(),null));
		assertThat(json(requisitar("GET","/produtos",a.token(),null)).path("total").asLong()).isEqualTo(CATALOGO);
		assertThat(lista.path("itens").size()).isPositive();
		var whey=lista.path("itens").get(0);assertThat(whey.path("nutrientes").path("energia_kcal").decimalValue()).isPositive();
		// a cópia é da nutri A: B não a acessa, e A pode desativar sem afetar B
		erro(requisitar("GET","/produtos/"+whey.path("id").asString(),b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		var m=new HashMap<String,Object>();m.put("nome",whey.path("nome").asString());m.put("nutrientes",new HashMap<>(Map.of("energia_kcal","1","carboidrato_g","1","proteina_g","1","lipideos_g","1")));m.put("ativo",false);
		assertThat(requisitar("PATCH","/produtos/"+whey.path("id").asString(),a.token(),m,ifMatch(whey.path("versao").asLong())).statusCode()).isEqualTo(200);
		assertThat(json(requisitar("GET","/produtos",a.token(),null)).path("total").asLong()).isEqualTo(CATALOGO-1);
		assertThat(json(requisitar("GET","/produtos",b.token(),null)).path("total").asLong()).isEqualTo(CATALOGO);
	}
}
