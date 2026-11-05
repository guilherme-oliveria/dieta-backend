package com.br.dietamais.pacientes.api;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** BE-66: próxima consulta e última avaliação na lista de pacientes da nutri, sem vazar dado de outra nutri. */
class ListaResumoApiTest extends TesteComPaciente {
	String agendar(String token,String ficha,Instant inicio) throws Exception {
		var m=new HashMap<String,Object>();m.put("pacienteId",ficha);m.put("inicio",inicio.truncatedTo(ChronoUnit.SECONDS).toString());m.put("duracaoMin",40);m.put("tipo","RETORNO");
		var r=requisitar("POST","/consultas",token,m);assertThat(r.statusCode()).as(r.body()).isEqualTo(201);return json(r).path("id").asString();
	}
	void avaliar(String token,String ficha,Instant dataHora,double peso) throws Exception {
		var m=new HashMap<String,Object>();m.put("dataHora",dataHora.toString());m.put("pesoKg",peso);m.put("estaturaM",1.75);
		assertThat(requisitar("POST","/pacientes/"+ficha+"/avaliacoes",token,m).statusCode()).isEqualTo(201);
	}
	JsonNode item(String token,String ficha) throws Exception {
		for(var i:json(requisitar("GET","/pacientes?tamanho=100",token,null)).path("itens"))if(ficha.equals(i.path("id").asString()))return i;
		throw new AssertionError("paciente fora da lista");
	}

	@Test void listaTrazProximaConsultaEUltimaAvaliacaoSoDaPropriaNutri() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var p=pacienteVinculado(a);var vazio=pacienteVinculado(a);
		var sem=item(a.token(),p.fichaId());assertThat(sem.has("proximaConsulta")).isFalse();assertThat(sem.has("ultimaAvaliacao")).isFalse();
		agendar(a.token(),p.fichaId(),Instant.now().plus(5,ChronoUnit.DAYS));
		var cedo=Instant.now().plus(2,ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);agendar(a.token(),p.fichaId(),cedo);
		var cancelada=agendar(a.token(),p.fichaId(),Instant.now().plus(1,ChronoUnit.DAYS));
		assertThat(requisitar("PATCH","/consultas/"+cancelada,a.token(),Map.of("status","CANCELADA"),Map.of("If-Match","\"1\"")).statusCode()).isEqualTo(200);
		avaliar(a.token(),p.fichaId(),Instant.now().minus(30,ChronoUnit.DAYS),80);
		avaliar(a.token(),p.fichaId(),Instant.now().minus(1,ChronoUnit.DAYS),70);
		var i=item(a.token(),p.fichaId());
		assertThat(Instant.parse(i.path("proximaConsulta").asString())).isEqualTo(cedo);
		assertThat(i.path("ultimaAvaliacao").path("pesoKg").decimalValue()).isEqualByComparingTo("70");
		assertThat(i.path("ultimaAvaliacao").path("imc").decimalValue()).isEqualByComparingTo("22.86");
		assertThat(i.path("ultimaAvaliacao").path("classificacaoImc").asString()).isEqualTo("NORMAL");
		// quem não tem consulta nem avaliação continua sem os campos; outra nutri não vê nada disso
		assertThat(item(a.token(),vazio.fichaId()).has("proximaConsulta")).isFalse();
		assertThat(json(requisitar("GET","/pacientes?tamanho=100",b.token(),null)).path("total").asLong()).isZero();
	}
}
