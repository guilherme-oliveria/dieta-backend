package com.br.dietamais.agenda;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.Test;

class ConsultasApiTest extends TesteComPaciente {
	Map<String,Object> corpo(String ficha,Instant inicio,int duracao,String tipo){
		var m=new HashMap<String,Object>();m.put("pacienteId",ficha);m.put("inicio",inicio.toString());m.put("duracaoMin",duracao);m.put("tipo",tipo);return m;
	}
	Instant amanha(){return Instant.now().plus(1,ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);}

	@Test void agendaValidaEIsolaPorNutriEPerfil() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var b=cadastrarNutri();
		var r=requisitar("POST","/consultas",a.token(),corpo(p.fichaId(),amanha(),50,"PRIMEIRA"));
		assertThat(r.statusCode()).as(r.body()).isEqualTo(201);var c=json(r);
		assertThat(c.path("status").asString()).isEqualTo("AGENDADA");assertThat(c.path("pacienteId").asString()).isEqualTo(p.fichaId());
		assertThat(c.path("duracaoMin").asInt()).isEqualTo(50);assertThat(r.headers().firstValue("ETag").orElseThrow()).isEqualTo("\"1\"");
		// validações
		erro(requisitar("POST","/consultas",a.token(),corpo(p.fichaId(),Instant.now().minus(1,ChronoUnit.DAYS),50,"RETORNO")),400,"FORMATO_INVALIDO");
		erro(requisitar("POST","/consultas",a.token(),corpo(p.fichaId(),Instant.now().plus(800,ChronoUnit.DAYS),50,"RETORNO")),400,"FORMATO_INVALIDO");
		erro(requisitar("POST","/consultas",a.token(),corpo(p.fichaId(),amanha(),4,"RETORNO")),400,"FORMATO_INVALIDO");
		erro(requisitar("POST","/consultas",a.token(),corpo(p.fichaId(),amanha(),481,"RETORNO")),400,"FORMATO_INVALIDO");
		var semTipo=corpo(p.fichaId(),amanha(),30,"RETORNO");semTipo.remove("tipo");erro(requisitar("POST","/consultas",a.token(),semTipo),400,"OBRIGATORIO");
		// isolamento: outra nutri não vê, não cria e não altera
		String id=c.path("id").asString();
		erro(requisitar("POST","/consultas",b.token(),corpo(p.fichaId(),amanha(),30,"RETORNO")),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("GET","/consultas?pacienteId="+p.fichaId(),b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("PATCH","/consultas/"+id,b.token(),Map.of("duracaoMin",30),Map.of("If-Match","\"1\"")),404,"RECURSO_NAO_ENCONTRADO");
		assertThat(json(requisitar("GET","/consultas",b.token(),null)).path("itens")).isEmpty();
		assertThat(requisitar("GET","/consultas",null,null).statusCode()).isEqualTo(401);
		// perfis: o paciente lê as suas e não usa a agenda da nutri
		assertThat(requisitar("GET","/consultas",p.token(),null).statusCode()).isEqualTo(403);
		erro(requisitar("GET","/eu/consultas",a.token(),null),403,"PERFIL_INVALIDO");
		erro(requisitar("POST","/consultas",p.token(),corpo(p.fichaId(),amanha(),30,"RETORNO")),403,"PERFIL_INVALIDO");
		var minhas=json(requisitar("GET","/eu/consultas",p.token(),null)).path("itens");
		assertThat(minhas).hasSize(1);assertThat(minhas.get(0).path("id").asString()).isEqualTo(id);
		assertThat(json(requisitar("GET","/consultas",a.token(),null)).path("itens")).hasSize(1);
	}

	@Test void janelaFiltrosEOrdemDaAgenda() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var q=pacienteVinculado(a);
		var tarde=Instant.now().plus(3,ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);var cedo=Instant.now().plus(2,ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
		requisitar("POST","/consultas",a.token(),corpo(p.fichaId(),tarde,30,"RETORNO"));requisitar("POST","/consultas",a.token(),corpo(q.fichaId(),cedo,30,"ONLINE"));
		var todas=json(requisitar("GET","/consultas",a.token(),null)).path("itens");
		assertThat(todas).hasSize(2);assertThat(todas.get(0).path("pacienteId").asString()).isEqualTo(q.fichaId()); // mais cedo primeiro
		assertThat(json(requisitar("GET","/consultas?pacienteId="+p.fichaId(),a.token(),null)).path("itens")).hasSize(1);
		var depois=Instant.now().plus(10,ChronoUnit.DAYS);
		assertThat(json(requisitar("GET","/consultas?de="+depois+"&ate="+depois.plus(5,ChronoUnit.DAYS),a.token(),null)).path("itens")).isEmpty();
		assertThat(json(requisitar("GET","/consultas?de="+cedo+"&ate="+cedo.plusSeconds(1),a.token(),null)).path("itens")).hasSize(1);
		erro(requisitar("GET","/consultas?de="+depois+"&ate="+depois,a.token(),null),400,"FORMATO_INVALIDO");
		erro(requisitar("GET","/consultas?de="+depois+"&ate="+depois.plus(400,ChronoUnit.DAYS),a.token(),null),400,"FORMATO_INVALIDO");
		assertThat(json(requisitar("GET","/eu/consultas",q.token(),null)).path("itens")).hasSize(1); // cada paciente só vê as suas
	}

	@Test void remarcaCancelaECongelaAConsultaCancelada() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);
		var c=json(requisitar("POST","/consultas",a.token(),corpo(p.fichaId(),amanha(),30,"RETORNO")));String id=c.path("id").asString();
		var novo=Instant.now().plus(2,ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
		var ok=requisitar("PATCH","/consultas/"+id,a.token(),Map.of("inicio",novo.toString(),"lugar","Sala 3"),Map.of("If-Match","\"1\""));
		assertThat(ok.statusCode()).as(ok.body()).isEqualTo(200);
		assertThat(json(ok).path("versao").asLong()).isEqualTo(2);assertThat(Instant.parse(json(ok).path("inicio").asString())).isEqualTo(novo);
		assertThat(json(ok).path("lugar").asString()).isEqualTo("Sala 3");assertThat(json(ok).path("duracaoMin").asInt()).isEqualTo(30); // ausente não muda
		var velho=requisitar("PATCH","/consultas/"+id,a.token(),Map.of("duracaoMin",45),Map.of("If-Match","\"1\""));
		erro(velho,409,"CONFLITO_VERSAO");assertThat(json(velho).path("atual").path("versao").asLong()).isEqualTo(2);
		erro(requisitar("PATCH","/consultas/"+id,a.token(),Map.of("duracaoMin",45)),400,"FORMATO_INVALIDO"); // sem If-Match
		erro(requisitar("PATCH","/consultas/"+id,a.token(),Map.of(),Map.of("If-Match","\"2\"")),400,"FORMATO_INVALIDO");
		erro(requisitar("PATCH","/consultas/"+id,a.token(),Map.of("inicio",Instant.now().minus(1,ChronoUnit.DAYS).toString()),Map.of("If-Match","\"2\"")),400,"FORMATO_INVALIDO");
		erro(requisitar("PATCH","/consultas/"+id,a.token(),Map.of("status","AGENDADA"),Map.of("If-Match","\"2\"")),400,"FORMATO_INVALIDO");
		erro(requisitar("PATCH","/consultas/"+id,a.token(),Map.of("status","CANCELADA","duracaoMin",45),Map.of("If-Match","\"2\"")),400,"FORMATO_INVALIDO");
		var limpa=requisitar("PATCH","/consultas/"+id,a.token(),Map.of("lugar",""),Map.of("If-Match","\"2\""));
		assertThat(limpa.statusCode()).as(limpa.body()).isEqualTo(200);assertThat(json(limpa).path("lugar").isNull()||json(limpa).path("lugar").isMissingNode()).isTrue();
		var cancelada=requisitar("PATCH","/consultas/"+id,a.token(),Map.of("status","CANCELADA"),Map.of("If-Match","\"3\""));
		assertThat(cancelada.statusCode()).as(cancelada.body()).isEqualTo(200);assertThat(json(cancelada).path("status").asString()).isEqualTo("CANCELADA");
		erro(requisitar("PATCH","/consultas/"+id,a.token(),Map.of("duracaoMin",45),Map.of("If-Match","\"4\"")),422,"CONSULTA_CANCELADA");
		erro(requisitar("PATCH","/consultas/"+id,a.token(),Map.of("status","CANCELADA"),Map.of("If-Match","\"4\"")),422,"CONSULTA_CANCELADA");
		assertThat(json(requisitar("GET","/eu/consultas",p.token(),null)).path("itens").get(0).path("status").asString()).isEqualTo("CANCELADA");
	}

	@Test void syncEntregaParaNutriEPacienteEOFeedIcsLevaAsConsultas() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);
		var cursor=json(requisitar("GET","/sync?limite=1000",p.token(),null)).path("cursor").asString();
		var c=json(requisitar("POST","/consultas",a.token(),corpo(p.fichaId(),amanha(),40,"ONLINE")));String id=c.path("id").asString();
		var paraPaciente=json(requisitar("GET","/sync?limite=1000&cursor="+cursor,p.token(),null)).path("mudancas").path("consultas");
		assertThat(paraPaciente).hasSize(1);assertThat(paraPaciente.get(0).path("id").asString()).isEqualTo(id);assertThat(paraPaciente.get(0).path("dados").path("tipo").asString()).isEqualTo("ONLINE");
		assertThat(json(requisitar("GET","/sync?limite=1000",a.token(),null)).path("mudancas").path("consultas")).hasSize(1);
		var token=json(requisitar("POST","/eu/calendario/token",p.token(),null)).path("url").asString();var caminho=token.substring(token.indexOf("/cal/"));
		var feed=publico(caminho).body();
		assertThat(feed).contains("BEGIN:VEVENT","UID:consulta-"+id+"@dietamais.com.br","SUMMARY:Dieta+: Consulta online","STATUS:CONFIRMED","SEQUENCE:0","DTSTART:").doesNotContain("RRULE");
		var depois=json(requisitar("GET","/sync?limite=1000",p.token(),null)).path("cursor").asString();
		assertThat(requisitar("PATCH","/consultas/"+id,a.token(),Map.of("lugar","https://meet.exemplo.invalid/sala"),Map.of("If-Match","\"1\"")).statusCode()).isEqualTo(200);
		assertThat(json(requisitar("GET","/sync?limite=1000&cursor="+depois,p.token(),null)).path("mudancas").path("consultas")).hasSize(1);
		assertThat(publico(caminho).body()).contains("SEQUENCE:1","LOCATION:https://meet.exemplo.invalid/sala");
		requisitar("PATCH","/consultas/"+id,a.token(),Map.of("status","CANCELADA"),Map.of("If-Match","\"2\""));
		assertThat(publico(caminho).body()).contains("STATUS:CANCELLED","SEQUENCE:2").doesNotContain("STATUS:CONFIRMED");
	}
}
