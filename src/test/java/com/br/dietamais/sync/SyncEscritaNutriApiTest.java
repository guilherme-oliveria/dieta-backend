package com.br.dietamais.sync;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class SyncEscritaNutriApiTest extends TesteComPaciente {
	String id(){return jdbc.queryForObject("select uuidv7()",String.class);}
	Map<String,Object> mut(String entidade,String operacao,String id,Long versaoBase,Object dados){
		var m=new HashMap<String,Object>();m.put("idMutacao",UUID.randomUUID().toString());m.put("entidade",entidade);m.put("operacao",operacao);m.put("id",id);m.put("versaoBase",versaoBase);m.put("dados",dados);return m;
	}
	JsonNode enviar(String token,Map<String,Object>... mutacoes) throws Exception {
		var r=requisitar("POST","/sync",token,Map.of("mutacoes",List.of(mutacoes)));assertThat(r.statusCode()).as(r.body()).isEqualTo(200);return json(r).path("resultados");
	}
	Map<String,Object> avaliacao(String ficha,String peso){return new HashMap<>(Map.of("pacienteId",ficha,"dataHora",Instant.now().minus(1,ChronoUnit.HOURS).toString(),"pesoKg",peso,"estaturaM","1.70"));}

	@Test void duasMutacoesNaMesmaAvaliacaoGeramExatamenteUmConflito() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);String av=id();
		var criar=enviar(a.token(),mut("avaliacoes","CRIAR",av,null,avaliacao(p.fichaId(),"70.0")));
		assertThat(criar.get(0).path("status").asString()).as(criar.toString()).isEqualTo("APLICADA");assertThat(criar.get(0).path("versao").asLong()).isEqualTo(1);
		var dadosA=avaliacao(p.fichaId(),"69.0");dadosA.remove("pacienteId");var dadosB=avaliacao(p.fichaId(),"68.0");dadosB.remove("pacienteId");
		var lote=enviar(a.token(),mut("avaliacoes","ATUALIZAR",av,1L,dadosA),mut("avaliacoes","ATUALIZAR",av,1L,dadosB));
		assertThat(lote.get(0).path("status").asString()).isEqualTo("APLICADA");assertThat(lote.get(0).path("versao").asLong()).isEqualTo(2);
		assertThat(lote.get(1).path("status").asString()).isEqualTo("CONFLITO");assertThat(lote.get(1).path("atual").path("versao").asLong()).isEqualTo(2);
		assertThat(lote.get(1).path("atual").path("pesoKg").decimalValue()).isEqualByComparingTo("69.0");
		long aplicadas=0,conflitos=0;for(var r:lote){if(r.path("status").asString().equals("APLICADA"))aplicadas++;if(r.path("status").asString().equals("CONFLITO"))conflitos++;}
		assertThat(aplicadas).isEqualTo(1);assertThat(conflitos).isEqualTo(1);
	}

	@Test void reenvioDaMesmaMutacaoDevolveOMesmoResultadoSemAplicarDeNovo() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);String av=id();var m=mut("avaliacoes","CRIAR",av,null,avaliacao(p.fichaId(),"70.0"));
		var r1=enviar(a.token(),m);var r2=enviar(a.token(),m);
		assertThat(r2.get(0)).isEqualTo(r1.get(0));
		assertThat(json(requisitar("GET","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),null)).path("itens")).hasSize(1);
	}

	@Test void regraViolada_rejeitaSoAqueleItemEIsolaPorNutri() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var b=cadastrarNutri();String av=id();
		enviar(a.token(),mut("avaliacoes","CRIAR",av,null,avaliacao(p.fichaId(),"70.0")));
		var dados=avaliacao(p.fichaId(),"70.0");dados.remove("pacienteId");
		var fora=Map.<String,Object>of("tipo","RETORNO");
		var futura=new HashMap<String,Object>(Map.of("pacienteId",p.fichaId(),"inicio",Instant.now().minus(2,ChronoUnit.DAYS).toString(),"duracaoMin",30,"tipo","RETORNO"));
		var boa=new HashMap<String,Object>(Map.of("pacienteId",p.fichaId(),"inicio",Instant.now().plus(2,ChronoUnit.DAYS).toString(),"duracaoMin",30,"tipo","RETORNO"));
		var rs=enviar(a.token(),
			mut("consultas","CRIAR",id(),null,futura),                     // 0: data no passado
			mut("consultas","CRIAR",id(),null,boa),                        // 1: ok mesmo depois da rejeitada
			mut("avaliacoes","ATUALIZAR",av,null,dados),                   // 2: sem versaoBase
			mut("avaliacoes","EXCLUIR",av,1L,null),                        // 3: não existe exclusão
			mut("avaliacoes","ATUALIZAR",id(),1L,dados),                   // 4: não existe
			mut("coisas","CRIAR",id(),null,fora));                         // 5: entidade desconhecida
		assertThat(rs.get(0).path("status").asString()).isEqualTo("REJEITADA");assertThat(rs.get(0).path("codigo").asString()).isEqualTo("FORMATO_INVALIDO");
		assertThat(rs.get(1).path("status").asString()).as(rs.toString()).isEqualTo("APLICADA");
		for(int i:new int[]{2,3,5})assertThat(rs.get(i).path("status").asString()+rs.get(i).path("codigo").asString()).isEqualTo("REJEITADAFORMATO_INVALIDO");
		assertThat(rs.get(4).path("codigo").asString()).isEqualTo("RECURSO_NAO_ENCONTRADO");
		// outra nutri não escreve em avaliação alheia nem em ficha alheia
		var rb=enviar(b.token(),mut("avaliacoes","ATUALIZAR",av,1L,dados),mut("avaliacoes","CRIAR",id(),null,avaliacao(p.fichaId(),"70.0")));
		assertThat(rb.get(0).path("codigo").asString()).isEqualTo("RECURSO_NAO_ENCONTRADO");assertThat(rb.get(1).path("codigo").asString()).isEqualTo("RECURSO_NAO_ENCONTRADO");
		// paciente não usa as entidades da nutri
		var rp=enviar(p.token(),mut("avaliacoes","CRIAR",id(),null,avaliacao(p.fichaId(),"70.0")));
		assertThat(rp.get(0).path("status").asString()).isEqualTo("REJEITADA");assertThat(rp.get(0).path("codigo").asString()).isEqualTo("PERFIL_INVALIDO");
	}

	@Test void fichaAlimentoModeloDietaListaEConsultaPeloSync() throws Exception {
		var a=cadastrarNutri();String ficha=id(),alimento=id(),modelo=id(),dieta=id(),consulta=id();
		var grupo=json(requisitar("GET","/grupos-alimentos",a.token(),null)).path("itens").get(0).path("id").asString();
		var rs=enviar(a.token(),
			mut("pacientes","CRIAR",ficha,null,Map.of("nome","Ficha offline")),
			mut("alimentos","CRIAR",alimento,null,Map.of("grupoId",grupo,"nome","Alimento offline","medidaCaseira","1 colher","gramasPorPorcao","30")));
		assertThat(rs.get(0).path("status").asString()).as(rs.toString()).isEqualTo("APLICADA");assertThat(rs.get(1).path("status").asString()).as(rs.toString()).isEqualTo("APLICADA");
		var rs2=enviar(a.token(),
			mut("pacientes","ATUALIZAR",ficha,0L,Map.of("nome","Ficha renomeada")),
			mut("alimentos","ATUALIZAR",alimento,0L,Map.of("grupoId",grupo,"nome","Alimento novo nome","medidaCaseira","1 colher","gramasPorPorcao","30","ativo",true)),
			mut("modelos","CRIAR",modelo,null,Map.of("nome","Modelo offline","blocos",List.of(Map.of("titulo","B","perguntas",List.of(Map.of("texto","Nova","tipo","TEXTO")))))),
			mut("dietas","CRIAR",dieta,null,Map.of("pacienteId",ficha,"titulo","Dieta offline","refeicoes",List.of(Map.of("nome","Café","horario","08:00:00","ordem",0,"itens",List.of(Map.of("alimentoId",alimento,"porcoes","1")))))),
			mut("consultas","CRIAR",consulta,null,Map.of("pacienteId",ficha,"inicio",Instant.now().plus(3,ChronoUnit.DAYS).toString(),"duracaoMin",40,"tipo","PRIMEIRA")));
		for(int i=0;i<rs2.size();i++)assertThat(rs2.get(i).path("status").asString()).as(i+" "+rs2).isEqualTo("APLICADA");
		assertThat(json(requisitar("GET","/pacientes/"+ficha,a.token(),null)).path("nome").asString()).isEqualTo("Ficha renomeada");
		// a dieta criada no sync é rascunho e a mesma regra do REST vale (publicada não edita)
		var rascunho=json(requisitar("GET","/dietas/"+dieta,a.token(),null));
		publicar(a.token(),rascunho);
		var bloqueada=enviar(a.token(),mut("dietas","ATUALIZAR",dieta,2L,Map.of("titulo","Mudou","refeicoes",List.of())));
		assertThat(bloqueada.get(0).path("status").asString()).isEqualTo("REJEITADA");
		// cancelar a consulta pelo sync
		var cancel=enviar(a.token(),mut("consultas","ATUALIZAR",consulta,1L,Map.of("status","CANCELADA")));
		assertThat(cancel.get(0).path("status").asString()).as(cancel.toString()).isEqualTo("APLICADA");
	}
}
