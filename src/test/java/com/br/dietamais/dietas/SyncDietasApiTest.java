package com.br.dietamais.dietas;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class SyncDietasApiTest extends TesteComPaciente {
	record Pagina(String cursor,Map<String,List<JsonNode>> mudancas) {}
	Pagina puxar(String token,String cursor) throws Exception {
		var mudancas=new LinkedHashMap<String,List<JsonNode>>();String atual=cursor;
		for(int i=0;i<50;i++){
			var r=requisitar("GET","/sync?limite=1000"+(atual==null?"":"&cursor="+atual),token,null);assertThat(r.statusCode()).as(r.body()).isEqualTo(200);var corpo=json(r);
			corpo.path("mudancas").properties().forEach(e->{var lista=mudancas.computeIfAbsent(e.getKey(),k->new ArrayList<>());e.getValue().forEach(lista::add);});
			atual=corpo.path("cursor").asString();if(!corpo.path("temMais").asBoolean())return new Pagina(atual,mudancas);
		}
		throw new AssertionError("sync não terminou");
	}
	List<String> ids(Pagina p,String colecao){return p.mudancas().getOrDefault(colecao,List.of()).stream().map(n->n.path("id").asString()).toList();}
	@Test void nutriRecebeSuaListaEDietasEPacienteRecebeSnapshotsSemVazar() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var p=pacienteVinculado(a);var ali=alimentos(a.token(),2);
		var inicial=puxar(a.token(),null);
		assertThat(ids(inicial,"grupos")).hasSize(15);assertThat(ids(inicial,"alimentos")).hasSize(482);assertThat(ids(inicial,"listas")).containsExactly(p.vinculoId());assertThat(ids(inicial,"dietas")).isEmpty();
		var dpb=puxar(b.token(),null);assertThat(ids(dpb,"listas")).isEmpty();assertThat(ids(dpb,"alimentos")).doesNotContainAnyElementsOf(ids(inicial,"alimentos"));assertThat(ids(dpb,"grupos")).isNotEmpty();
		var d=criarDieta(a.token(),p.fichaId(),dieta("Sync",refeicao("R",0,item(ali.get(0),"1"))));
		var depoisRascunho=puxar(a.token(),inicial.cursor());assertThat(ids(depoisRascunho,"dietas")).containsExactly(d.path("id").asString());assertThat(depoisRascunho.mudancas().get("dietas").get(0).path("dados").path("status").asString()).isEqualTo("RASCUNHO");
		assertThat(ids(depoisRascunho,"alimentos")).isEmpty();
		var pacienteInicial=puxar(p.token(),null);assertThat(ids(pacienteInicial,"snapshots")).isEmpty();assertThat(ids(pacienteInicial,"dietas")).isEmpty();assertThat(ids(pacienteInicial,"alimentos")).isEmpty();assertThat(ids(pacienteInicial,"listas")).isEmpty();
		var s=publicar(a.token(),d);
		var pacienteDepois=puxar(p.token(),pacienteInicial.cursor());
		assertThat(ids(pacienteDepois,"snapshots")).containsExactly(s.path("id").asString());assertThat(pacienteDepois.mudancas().get("snapshots").get(0).path("dados")).isEqualTo(s);
		assertThat(ids(pacienteDepois,"dietaEmUso")).hasSize(1);
		assertThat(pacienteDepois.mudancas().get("dietaEmUso").get(0).path("dados").path("dietaId").asString()).isEqualTo(d.path("id").asString());
		// a outra nutri e o outro caminho nunca enxergam
		assertThat(ids(puxar(b.token(),dpb.cursor()),"dietas")).isEmpty();
		// edição de alimento chega só para a dona, junto com as listas revisadas
		var edicao=requisitar("PATCH","/alimentos/"+ali.get(1).path("id").asString(),a.token(),Map.of("grupoId",ali.get(1).path("grupoId").asString(),"nome","Nome sync","medidaCaseira","1 un","gramasPorPorcao",10,"ativo",true),Map.of("If-Match","\""+ali.get(1).path("versao").asLong()+"\""));
		assertThat(edicao.statusCode()).as(edicao.body()).isEqualTo(200);
		var aposEdicao=puxar(a.token(),depoisRascunho.cursor());assertThat(ids(aposEdicao,"alimentos")).contains(ali.get(1).path("id").asString());assertThat(ids(aposEdicao,"listas")).containsExactly(p.vinculoId());
		assertThat(ids(puxar(b.token(),dpb.cursor()),"alimentos")).isEmpty();
	}
	@Test void dietaPublicadaAntesDoAceiteChegaAoPacienteComIdGlobal() throws Exception {
		var a=cadastrarNutri();var ali=alimentos(a.token(),1);
		String email=UUID.randomUUID()+"@example.invalid";
		var ficha=json(requisitar("POST","/pacientes",a.token(),Map.of("nome","Paciente fictício","email",email)));String fichaId=ficha.path("id").asString();
		var d=criarDieta(a.token(),fichaId,dieta("Antes do aceite",refeicao("R",0,item(ali.get(0),"1"))));var s=publicar(a.token(),d);
		String codigo=json(requisitar("POST","/pacientes/"+fichaId+"/convite",a.token(),null)).path("codigo").asString();
		var corpo=new HashMap<String,Object>();corpo.put("aceitouTermo",true);corpo.put("versaoTermo","clinico-teste-v1");corpo.put("dispositivo",Map.of("id",UUID.randomUUID(),"plataforma","ANDROID","nomeAparelho","Aparelho fictício"));
		corpo.put("cadastro",Map.of("nome","Paciente fictício","email",email,"senha",SENHA,"aceitouTermos",true,"versaoTermos","2026-10"));
		var aceite=requisitar("POST","/convites/"+codigo+"/aceitar",null,corpo);assertThat(aceite.statusCode()).as(aceite.body()).isEqualTo(200);
		String token=json(aceite).path("sessao").path("accessToken").asString();
		UUID global=jdbc.queryForObject("select paciente_id from vinculo where id=?",UUID.class,UUID.fromString(s.path("vinculoId").asString()));
		var lista=json(requisitar("GET","/eu/dietas",token,null));assertThat(lista.path("itens")).hasSize(1);
		assertThat(lista.path("itens").get(0).path("pacienteId").asString()).isEqualTo(global.toString());assertThat(lista.path("itens").get(0).path("listaSubstituicao").path("pacienteId").asString()).isEqualTo(global.toString());
		assertThat(lista.path("dietaEmUsoId").asString()).isEqualTo(d.path("id").asString());
		var sync=puxar(token,null);assertThat(ids(sync,"snapshots")).containsExactly(s.path("id").asString());assertThat(sync.mudancas().get("snapshots").get(0).path("dados").path("pacienteId").asString()).isEqualTo(global.toString());
		assertThat(ids(sync,"dietaEmUso")).hasSize(1);
		// a nutri continua editando o rascunho por qualquer id de paciente atual
		var nova=criarDieta(a.token(),global.toString(),dieta("Depois",refeicao("R",0,item(ali.get(0),"2"))));
		var put=requisitar("PUT","/dietas/"+nova.path("id").asString(),a.token(),dieta("Depois 2",refeicao("R",0,item(ali.get(0),"1"))),Map.of("If-Match","\"1\""));assertThat(put.statusCode()).as(put.body()).isEqualTo(200);
		assertThat(jdbc.queryForObject("select paciente_id from dieta where id=?",UUID.class,UUID.fromString(d.path("id").asString()))).isEqualTo(global);
	}
}
