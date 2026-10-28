package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class EvolucaoApiTest extends TesteComPaciente {
	Map<String,Object> avaliacao(int diasAtras,Object peso,Object cintura,boolean compartilhada){
		var m=new HashMap<String,Object>();m.put("dataHora",Instant.now().minus(diasAtras,ChronoUnit.DAYS).toString());m.put("pesoKg",peso);m.put("estaturaM",1.70);
		if(cintura!=null)m.put("perimetros",Map.of("cintura",cintura));m.put("compartilhada",compartilhada);return m;
	}
	JsonNode serie(JsonNode resposta,String medida){
		for(var s:resposta.path("series"))if(s.path("medida").asString().equals(medida))return s;return null;
	}
	@Test void seriesEmOrdemCronologicaComIsolamentoEVisibilidade() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var b=cadastrarNutri();
		assertThat(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),avaliacao(1,70,80,true)).statusCode()).isEqualTo(201);
		assertThat(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),avaliacao(30,74,85,true)).statusCode()).isEqualTo(201);
		assertThat(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),avaliacao(10,72,null,false)).statusCode()).isEqualTo(201);
		assertThat(requisitar("POST","/eu/pesos",p.token(),Map.of("pesoKg",71.5,"registradoEm",Instant.now().minusSeconds(60).toString())).statusCode()).isEqualTo(201);

		var nutri=json(requisitar("GET","/pacientes/"+p.fichaId()+"/evolucao",a.token(),null));
		var peso=serie(nutri,"PESO");assertThat(peso.path("unidade").asString()).isEqualTo("kg");
		assertThat(peso.path("pontos")).hasSize(3);
		assertThat(peso.path("pontos").get(0).path("valor").decimalValue()).isEqualByComparingTo("74");
		assertThat(peso.path("pontos").get(2).path("valor").decimalValue()).isEqualByComparingTo("70");
		assertThat(serie(nutri,"PERIMETRO_CINTURA").path("pontos")).hasSize(2);
		assertThat(serie(nutri,"PESO_EM_CASA").path("pontos")).hasSize(1);
		assertThat(serie(nutri,"PERCENTUAL_GORDURA")).isNull();

		// o paciente não vê a avaliação privada
		var minha=json(requisitar("GET","/eu/evolucao",p.token(),null));
		assertThat(serie(minha,"PESO").path("pontos")).hasSize(2);assertThat(serie(minha,"PESO_EM_CASA").path("pontos")).hasSize(1);

		// outra nutri: 404; perfis trocados: 403
		erro(requisitar("GET","/pacientes/"+p.fichaId()+"/evolucao",b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("GET","/eu/evolucao",a.token(),null),403,"PERFIL_INVALIDO");
		erro(requisitar("GET","/pacientes/"+p.fichaId()+"/evolucao",p.token(),null),403,"PERFIL_INVALIDO");
		assertThat(requisitar("GET","/eu/evolucao",null,null).statusCode()).isEqualTo(401);

		// vínculo encerrado: a nutri mantém as avaliações, mas perde o peso em casa
		assertThat(requisitar("POST","/pacientes/"+p.fichaId()+"/encerrar-vinculo",a.token(),null).statusCode()).isEqualTo(200);
		var encerrado=json(requisitar("GET","/pacientes/"+p.fichaId()+"/evolucao",a.token(),null));
		assertThat(serie(encerrado,"PESO").path("pontos")).hasSize(3);assertThat(serie(encerrado,"PESO_EM_CASA")).isNull();
	}
	@Test void semDadosDevolveListaVazia() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);
		assertThat(json(requisitar("GET","/pacientes/"+p.fichaId()+"/evolucao",a.token(),null)).path("series")).isEmpty();
		assertThat(json(requisitar("GET","/eu/evolucao",p.token(),null)).path("series")).isEmpty();
	}
}
