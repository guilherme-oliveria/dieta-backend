package com.br.dietamais.rotina;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class AdesaoApiTest extends TesteComPaciente {
	static final ZoneId FUSO=ZoneId.of("America/Sao_Paulo");
	void checkin(String token,JsonNode snapshot,String status,LocalDate data) throws Exception {
		var m=new HashMap<String,Object>();m.put("dietaId",snapshot.path("dietaId").asString());m.put("refeicaoId",snapshot.path("refeicoes").get(0).path("id").asString());
		m.put("data",data.toString());m.put("status",status);m.put("registradoEm",Instant.now().toString());
		var r=requisitar("POST","/eu/checkins",token,m);assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
	}
	JsonNode ficha(String token,String pacienteId) throws Exception {
		for(var i:json(requisitar("GET","/pacientes",token,null)).path("itens"))if(i.path("id").asString().equals(pacienteId))return i;
		throw new AssertionError("paciente fora da lista");
	}
	@Test void adesaoSobreRefeicoesEsperadasEIsolamento() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);
		var semDieta=requisitar("GET","/pacientes/"+p.fichaId()+"/adesao",a.token(),null);
		assertThat(semDieta.statusCode()).isEqualTo(200);assertThat(json(semDieta).path("dias7").path("percentual").isNull()).isTrue();
		assertThat(ficha(a.token(),p.fichaId()).path("adesao7d").isNull()).isTrue();
		var ali=alimentos(a.token(),1);
		var s=publicar(a.token(),criarDieta(a.token(),p.fichaId(),dieta("Adesão",refeicao("Café",0,item(ali.get(0),"1")))));
		var antes=json(requisitar("GET","/pacientes/"+p.fichaId()+"/adesao",a.token(),null));
		assertThat(antes.path("dias7").path("esperadas").asInt()).isEqualTo(1);assertThat(antes.path("dias7").path("percentual").decimalValue()).isEqualByComparingTo("0");
		checkin(p.token(),s,"FEITO",LocalDate.now(FUSO));
		var depois=json(requisitar("GET","/pacientes/"+p.fichaId()+"/adesao",a.token(),null));
		assertThat(depois.path("dias7").path("feitas").asInt()).isEqualTo(1);assertThat(depois.path("dias7").path("percentual").decimalValue()).isEqualByComparingTo("100");
		assertThat(depois.path("dias30").path("percentual").decimalValue()).isEqualByComparingTo("100");
		assertThat(ficha(a.token(),p.fichaId()).path("adesao7d").decimalValue()).isEqualByComparingTo("100");
		checkin(p.token(),s,"PULADO",LocalDate.now(FUSO)); // o último registro do dia vale
		var pulou=json(requisitar("GET","/pacientes/"+p.fichaId()+"/adesao",a.token(),null));
		assertThat(pulou.path("dias7").path("puladas").asInt()).isEqualTo(1);assertThat(pulou.path("dias7").path("feitas").asInt()).isZero();
		assertThat(ficha(a.token(),p.fichaId()).path("adesao7d").decimalValue()).isEqualByComparingTo("0");
		var outra=cadastrarNutri();
		erro(requisitar("GET","/pacientes/"+p.fichaId()+"/adesao",outra.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("GET","/pacientes/"+p.fichaId()+"/adesao",p.token(),null),403,"PERFIL_INVALIDO");
		assertThat(requisitar("GET","/pacientes/"+p.fichaId()+"/adesao",null,null).statusCode()).isEqualTo(401);
	}
}
