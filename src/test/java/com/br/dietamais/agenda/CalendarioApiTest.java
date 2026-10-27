package com.br.dietamais.agenda;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import org.junit.jupiter.api.Test;

class CalendarioApiTest extends TesteComPaciente {
	String caminho(String url){return url.substring(url.indexOf("/cal/"));}
	@Test void feedPublicoPorTokenComRefeicoesDaDietaEmUsoERevogacao() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var ali=alimentos(a.token(),1);
		erro(requisitar("POST","/eu/calendario/token",a.token(),null),403,"PERFIL_INVALIDO");
		assertThat(requisitar("POST","/eu/calendario/token",null,null).statusCode()).isEqualTo(401);
		var t1=json(requisitar("POST","/eu/calendario/token",p.token(),null));
		assertThat(t1.path("token").asString()).hasSize(43);assertThat(t1.path("urlWebcal").asString()).startsWith("webcal://");
		var vazio=publico(caminho(t1.path("url").asString()));
		assertThat(vazio.statusCode()).isEqualTo(200);assertThat(vazio.body()).contains("BEGIN:VCALENDAR").doesNotContain("VEVENT");
		var s=publicar(a.token(),criarDieta(a.token(),p.fichaId(),dieta("Plano",refeicao("Café da manhã",0,item(ali.get(0),"1")))));
		var cheio=publico(caminho(t1.path("url").asString()));
		assertThat(cheio.headers().firstValue("Content-Type").orElse("")).startsWith("text/calendar");
		assertThat(cheio.body()).contains("BEGIN:VEVENT","UID:refeicao-"+s.path("refeicoes").get(0).path("id").asString()+"@dietamais.com.br","RRULE:FREQ=DAILY","SUMMARY:Dieta+: Café da manhã","BEGIN:VTIMEZONE","TZID=America/Sao_Paulo","END:VCALENDAR")
			.doesNotContain(ali.get(0).toString()).contains("\r\n");
		var t2=json(requisitar("POST","/eu/calendario/token",p.token(),null));
		assertThat(t2.path("token").asString()).isNotEqualTo(t1.path("token").asString());
		assertThat(publico(caminho(t1.path("url").asString())).statusCode()).isEqualTo(404);
		assertThat(publico(caminho(t2.path("url").asString())).statusCode()).isEqualTo(200);
		assertThat(requisitar("DELETE","/eu/calendario/token",p.token(),null).statusCode()).isEqualTo(204);
		assertThat(publico(caminho(t2.path("url").asString())).statusCode()).isEqualTo(404);
		assertThat(publico("/cal/curto.ics").statusCode()).isEqualTo(404);
		assertThat(publico("/cal/"+"a".repeat(43)+".ics").statusCode()).isEqualTo(404);
	}
}
