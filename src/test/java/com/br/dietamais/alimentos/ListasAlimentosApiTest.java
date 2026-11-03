package com.br.dietamais.alimentos;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.TesteIsolamento;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class ListasAlimentosApiTest extends TesteIsolamento {
	static final AtomicLong IDS=new AtomicLong(1000);
	@Test void macrosSaoEditaveisSomenteNaCopiaPropriaComOrigemEVersao() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var inicial=grupos(a.token()).get(0);String id=inicial.path("id").asString();
		var base=jdbc.queryForMap("select carboidratos_por_porcao,proteinas_por_porcao,gorduras_por_porcao,kcal_por_porcao from grupo_alimento where id=?",UUID.fromString(id));
		var resposta=requisitar("PATCH","/grupos-alimentos/"+id,a.token(),Map.of("carboidratosPorPorcao",30,"proteinasPorPorcao",5,"gordurasPorPorcao",2),Map.of("If-Match",tag(inicial)));
		assertThat(resposta.statusCode()).isEqualTo(200);var alterado=json(resposta);assertThat(alterado.path("origemMacros").asString()).isEqualTo("AJUSTE_NUTRI");
		assertThat(alterado.has("macrosEstimados")).isFalse();assertThat(alterado.path("versao").asLong()).isEqualTo(inicial.path("versao").asLong()+1);
		assertThat(alterado.path("kcalPorPorcao")).isEqualTo(inicial.path("kcalPorPorcao"));assertThat(resposta.headers().firstValue("ETag").orElseThrow()).isEqualTo(tag(alterado));
		assertThat(jdbc.queryForMap("select carboidratos_por_porcao,proteinas_por_porcao,gorduras_por_porcao,kcal_por_porcao from grupo_alimento where id=?",UUID.fromString(id))).isEqualTo(base);
		assertThat(grupos(b.token()).get(0)).isEqualTo(inicial);
		assertThat(jdbc.queryForObject("select ajustado_por from grupo_alimento_nutri where nutricionista_id=? and grupo_id=?",UUID.class,a.nutri(),UUID.fromString(id))).isEqualTo(a.usuario());
		assertThat(jdbc.queryForObject("select count(*) from historico_macros_grupo where nutricionista_id=? and grupo_id=? and usuario_id=?",Integer.class,a.nutri(),UUID.fromString(id),a.usuario())).isEqualTo(1);
		erro(requisitar("PATCH","/grupos-alimentos/"+id,a.token(),Map.of("carboidratosPorPorcao",1,"proteinasPorPorcao",1,"gordurasPorPorcao",1),Map.of("If-Match",tag(inicial))),409,"CONFLITO_VERSAO");
		erro(requisitar("PATCH","/grupos-alimentos/"+UUID.randomUUID(),a.token(),Map.of("carboidratosPorPorcao",1,"proteinasPorPorcao",1,"gordurasPorPorcao",1),Map.of("If-Match","\"0\"")),404,"RECURSO_NAO_ENCONTRADO");
	}
	@Test void crudBuscaLiteralIdempotenciaEDesativacaoSaoIsolados() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();String grupo=grupos(a.token()).get(0).path("id").asString();var corpo=novo(grupo,"Alimento %_ ficticio");String chave=UUID.randomUUID().toString();
		var criado=requisitar("POST","/alimentos",a.token(),corpo,Map.of("Idempotency-Key",chave));assertThat(criado.statusCode()).isEqualTo(201);var alimento=json(criado);String id=alimento.path("id").asString();
		var repetido=requisitar("POST","/alimentos",a.token(),corpo,Map.of("Idempotency-Key",chave));assertThat(repetido.statusCode()).isEqualTo(201);assertThat(json(repetido)).isEqualTo(alimento);
		assertThat(json(requisitar("GET","/alimentos?busca=%25_",a.token(),null)).path("total").asLong()).isEqualTo(1);
		assertThat(json(requisitar("GET","/alimentos?busca=%25_",b.token(),null)).path("total").asLong()).isZero();
		for(int i=0;i<2;i++)assertThat(requisitar("POST","/alimentos",a.token(),novo(grupo,"Empate ficticio")).statusCode()).isEqualTo(201);
		var pagina0=json(requisitar("GET","/alimentos?busca=Empate%20ficticio&tamanho=1&pagina=0",a.token(),null));
		var pagina1=json(requisitar("GET","/alimentos?busca=Empate%20ficticio&tamanho=1&pagina=1",a.token(),null));
		assertThat(pagina0.path("total").asLong()).isEqualTo(2);assertThat(pagina0.path("itens").get(0).path("id")).isNotEqualTo(pagina1.path("itens").get(0).path("id"));
		assertThat(json(requisitar("GET","/alimentos?busca=Empate%20ficticio&tamanho=1&pagina=0",a.token(),null))).isEqualTo(pagina0);
		erro(requisitar("PATCH","/alimentos/"+id,b.token(),editar(alimento,false),Map.of("If-Match",tag(alimento))),404,"RECURSO_NAO_ENCONTRADO");
		var alterado=requisitar("PATCH","/alimentos/"+id,a.token(),editar(alimento,false),Map.of("If-Match",tag(alimento)));assertThat(alterado.statusCode()).isEqualTo(200);assertThat(json(alterado).path("ativo").asBoolean()).isFalse();
		assertThat(json(alterado).path("versao").asLong()).isEqualTo(alimento.path("versao").asLong()+1);
		assertThat(json(requisitar("GET","/alimentos?busca=%25_",a.token(),null)).path("total").asLong()).isZero();
		assertThat(json(requisitar("GET","/alimentos?ativo=false&busca=%25_",a.token(),null)).path("total").asLong()).isEqualTo(1);
		assertThat(jdbc.queryForObject("select count(*) from alimento_nutri where id=?",Integer.class,UUID.fromString(id))).isEqualTo(1);
		erro(requisitar("PATCH","/alimentos/"+id,a.token(),editar(alimento,true),Map.of("If-Match",tag(alimento))),409,"CONFLITO_VERSAO");
		erro(requisitar("GET","/alimentos?pagina=-1",a.token(),null),400,"FORMATO_INVALIDO");
	}
	@Test void diferencasPorPacienteNaoAfetamOutroPacienteEHistoricoSoPermiteLeitura() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var p=criarPaciente(a.token());var outro=criarPaciente(a.token());String pid=p.path("id").asString();String outroId=outro.path("id").asString();
		var antes=lista(a.token(),pid);String grupo=grupos(a.token()).get(0).path("id").asString();var novo=requisitar("POST","/alimentos",a.token(),novo(grupo,"Somente um paciente ficticio"));assertThat(novo.statusCode()).isEqualTo(201);String food=json(novo).path("id").asString();
		assertThat(contem(lista(a.token(),pid),food)).isFalse();assertThat(contem(lista(a.token(),outroId),food)).isFalse();
		erro(requisitar("PUT","/pacientes/"+pid+"/lista-substituicao",a.token(),Map.of("adicionados",List.of(food),"removidos",List.of()),Map.of("If-Match",tag(antes))),409,"CONFLITO_VERSAO");
		var atual=lista(a.token(),pid);var escrita=requisitar("PUT","/pacientes/"+pid+"/lista-substituicao",a.token(),Map.of("adicionados",List.of(food),"removidos",List.of()),Map.of("If-Match",tag(atual)));assertThat(escrita.statusCode()).isEqualTo(200);
		assertThat(contem(json(escrita),food)).isTrue();assertThat(contem(lista(a.token(),outroId),food)).isFalse();
		String estrangeiro=json(requisitar("GET","/alimentos",b.token(),null)).path("itens").get(0).path("id").asString();
		erro(requisitar("PUT","/pacientes/"+pid+"/lista-substituicao",a.token(),Map.of("adicionados",List.of(estrangeiro),"removidos",List.of()),Map.of("If-Match",tag(json(escrita)))),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("GET","/pacientes/"+pid+"/lista-substituicao",b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("PUT","/pacientes/"+pid+"/lista-substituicao",a.token(),Map.of("adicionados",List.of(food,food),"removidos",List.of()),Map.of("If-Match",tag(json(escrita)))),400,"FORMATO_INVALIDO");
		assertThat(requisitar("POST","/pacientes/"+pid+"/encerrar-vinculo",a.token(),null).statusCode()).isEqualTo(200);
		var historico=lista(a.token(),pid);assertThat(contem(historico,food)).isTrue();
		erro(requisitar("PUT","/pacientes/"+pid+"/lista-substituicao",a.token(),Map.of("adicionados",List.of(),"removidos",List.of()),Map.of("If-Match",tag(historico))),403,"ACESSO_NEGADO");
		assertThat(jdbc.queryForObject("select count(*) from log_acesso where usuario_id=? and registro_id=? and entidade='lista_substituicao' and acao='ESCRITA'",Integer.class,a.usuario(),UUID.fromString(p.path("vinculoId").asString()))).isEqualTo(1);
	}
	@Test void pacienteGlobalCompartilhadoMantemListasPorVinculo() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var pa=criarPaciente(a.token());var pb=criarPaciente(b.token());UUID pid=UUID.fromString(pa.path("id").asString());UUID vb=UUID.fromString(pb.path("vinculoId").asString());
		jdbc.update("update vinculo set paciente_id=? where id=?",pid,vb);jdbc.update("update ficha_paciente set paciente_id=? where vinculo_id=?",pid,vb);
		var la=lista(a.token(),pid.toString());var lb=lista(b.token(),pid.toString());String food=la.path("alimentos").get(0).path("id").asString();
		assertThat(contem(lb,food)).isFalse();
		var resposta=requisitar("PUT","/pacientes/"+pid+"/lista-substituicao",a.token(),Map.of("adicionados",List.of(),"removidos",List.of(food)),Map.of("If-Match",tag(la)));assertThat(resposta.statusCode()).isEqualTo(200);
		assertThat(contem(json(resposta),food)).isFalse();assertThat(lista(b.token(),pid.toString())).isEqualTo(lb);
	}
	@Test void concorrenciaConflitoEAssinaturaNaoSobrescrevemDados() throws Exception {
		var a=cadastrarNutri();String grupo=grupos(a.token()).get(0).path("id").asString();var criado=json(requisitar("POST","/alimentos",a.token(),novo(grupo,"Concorrente ficticio")));var largada=new CountDownLatch(1);
		try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
			var tarefas=new ArrayList<Future<Integer>>();for(boolean ativo:new boolean[]{true,false})tarefas.add(executor.submit(()->{largada.await();return requisitar("PATCH","/alimentos/"+criado.path("id").asString(),a.token(),editar(criado,ativo),Map.of("If-Match",tag(criado))).statusCode();}));
			largada.countDown();assertThat(List.of(tarefas.get(0).get(15,TimeUnit.SECONDS),tarefas.get(1).get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
		}
		jdbc.update("update assinatura set status='CANCELADA' where organizacao_id=?",a.organizacao());
		erro(requisitar("POST","/alimentos",a.token(),novo(grupo,"Bloqueado ficticio")),422,"ASSINATURA_INATIVA");assertThat(requisitar("GET","/alimentos",a.token(),null).statusCode()).isEqualTo(200);
		jdbc.update("insert into paciente(usuario_id,nome) values(?,'Paciente fictício')",a.usuario());
		erro(requisitar("GET","/alimentos",entrar(a.email(),"PACIENTE"),null),403,"PERFIL_INVALIDO");
	}
	@Test void inicializacaoDeLegadoEOcorrenciaUnicaPreservamAjustes() throws Exception {
		var a=cadastrarNutri();jdbc.update("delete from alimento_nutri where nutricionista_id=?",a.nutri());jdbc.update("delete from grupo_alimento_nutri where nutricionista_id=?",a.nutri());jdbc.update("delete from lista_alimentos_nutri where nutricionista_id=?",a.nutri());
		jdbc.queryForObject("select copiar_catalogo_nutri(?)",Object.class,a.nutri());assertThat(jdbc.queryForObject("select count(*) from alimento_nutri where nutricionista_id=?",Integer.class,a.nutri())).isEqualTo(482);
		UUID id=jdbc.queryForObject("select id from alimento_nutri where nutricionista_id=? order by id limit 1",UUID.class,a.nutri());jdbc.update("update alimento_nutri set nome='Ajuste fictício preservado' where id=?",id);
		jdbc.queryForObject("select copiar_catalogo_nutri(?)",Object.class,a.nutri());assertThat(jdbc.queryForObject("select nome from alimento_nutri where id=?",String.class,id)).isEqualTo("Ajuste fictício preservado");
	}
	JsonNode grupos(String token) throws Exception {var r=requisitar("GET","/grupos-alimentos",token,null);assertThat(r.statusCode()).isEqualTo(200);return json(r).path("itens");}
	JsonNode lista(String token,String pid) throws Exception {var r=requisitar("GET","/pacientes/"+pid+"/lista-substituicao",token,null);assertThat(r.statusCode()).isEqualTo(200);return json(r);}
	JsonNode criarPaciente(String token) throws Exception {var r=requisitar("POST","/pacientes",token,Map.of("nome","Paciente fictício","email",UUID.randomUUID()+"@example.invalid"));assertThat(r.statusCode()).isEqualTo(201);return json(r);}
	boolean contem(JsonNode lista,String id){for(var a:lista.path("alimentos"))if(a.path("id").asString().equals(id))return true;return false;}
	String tag(JsonNode valor){return "\""+valor.path("versao").asLong()+"\"";}
	Map<String,Object> novo(String grupo,String nome){return Map.of("id",String.format("019a0000-0002-7000-8000-%012d",IDS.incrementAndGet()),"grupoId",grupo,"nome",nome,"medidaCaseira","1 unidade fictícia","gramasPorPorcao",new BigDecimal("10.125"));}
	Map<String,Object> editar(JsonNode alimento,boolean ativo){return Map.of("grupoId",alimento.path("grupoId").asString(),"nome",alimento.path("nome").asString(),"medidaCaseira",alimento.path("medidaCaseira").asString(),"gramasPorPorcao",alimento.path("gramasPorPorcao").decimalValue(),"ativo",ativo);}
}
