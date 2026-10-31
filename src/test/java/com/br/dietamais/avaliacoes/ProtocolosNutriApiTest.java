package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

/** BE-42: "Meus protocolos" (RN-CAL-03): padrão por nutri, versão, isolamento, sync e efeito nas avaliações novas. */
class ProtocolosNutriApiTest extends TesteComPaciente {
	Map<String,String> se(long versao){return Map.of("If-Match","\""+versao+"\"");}
	Map<String,Object> avaliacao(){
		var m=new HashMap<String,Object>();m.put("dataHora",Instant.now().minusSeconds(60).toString());m.put("pesoKg",102.5);m.put("estaturaM",1.93);
		m.put("dobras",Map.of("triceps",20,"peitoral",25,"axilarMedia",28,"subescapular",30,"abdominal",40,"suprailiaca",32,"coxa",24));return m;
	}
	PacienteTeste paciente(ContaTeste nutri) throws Exception {
		var p=pacienteVinculado(nutri);
		jdbc.update("update ficha_paciente set sexo='MASCULINO',data_nascimento=? where vinculo_id=?::uuid",java.sql.Date.valueOf(LocalDate.now().minusYears(29).minusMonths(6)),p.vinculoId());return p;
	}

	@Test void semSalvarVemOPadraoNaVersaoZero() throws Exception {
		var a=cadastrarNutri();var r=requisitar("GET","/eu/nutri/protocolos",a.token(),null);
		assertThat(r.statusCode()).isEqualTo(200);assertThat(r.headers().firstValue("ETag")).hasValue("\"0\"");
		var j=json(r);assertThat(j.path("dobras").asString()).isEqualTo("SETE");assertThat(j.path("massa").asString()).isEqualTo("MAGRO");assertThat(j.path("versao").asLong()).isZero();
	}
	@Test void salvaComVersaoEConflitaComVersaoVelha() throws Exception {
		var a=cadastrarNutri();
		var primeira=requisitar("PUT","/eu/nutri/protocolos",a.token(),Map.of("dobras","TRES","massa","MUSCULAR"),se(0));
		assertThat(primeira.statusCode()).as(primeira.body()).isEqualTo(200);assertThat(json(primeira).path("versao").asLong()).isEqualTo(1);
		var velha=requisitar("PUT","/eu/nutri/protocolos",a.token(),Map.of("dobras","SETE","massa","MAGRO"),se(0));
		assertThat(velha.statusCode()).isEqualTo(409);assertThat(json(velha).path("codigo").asString()).isEqualTo("CONFLITO_VERSAO");
		var segunda=requisitar("PUT","/eu/nutri/protocolos",a.token(),Map.of("dobras","SETE","massa","MAGRO"),se(1));
		assertThat(json(segunda).path("versao").asLong()).isEqualTo(2);
		assertThat(json(requisitar("GET","/eu/nutri/protocolos",a.token(),null)).path("dobras").asString()).isEqualTo("SETE");
	}
	@Test void valorDesconhecidoEhProtocoloInvalido() throws Exception {
		var a=cadastrarNutri();
		var r=requisitar("PUT","/eu/nutri/protocolos",a.token(),Map.of("dobras","QUATRO","massa","MAGRO"),se(0));
		assertThat(r.statusCode()).isEqualTo(422);assertThat(json(r).path("codigo").asString()).isEqualTo("PROTOCOLO_INVALIDO");
		assertThat(requisitar("PUT","/eu/nutri/protocolos",a.token(),Map.of("dobras","TRES"),se(0)).statusCode()).isEqualTo(400);
	}
	@Test void cadaNutriTemOsSeusEPacienteNaoAcessa() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var p=pacienteVinculado(a);
		requisitar("PUT","/eu/nutri/protocolos",a.token(),Map.of("dobras","TRES","massa","MUSCULAR"),se(0));
		var deB=json(requisitar("GET","/eu/nutri/protocolos",b.token(),null));
		assertThat(deB.path("dobras").asString()).isEqualTo("SETE");assertThat(deB.path("versao").asLong()).isZero();
		assertThat(requisitar("GET","/eu/nutri/protocolos",p.token(),null).statusCode()).isEqualTo(403);
		assertThat(requisitar("PUT","/eu/nutri/protocolos",p.token(),Map.of("dobras","TRES","massa","MAGRO"),se(0)).statusCode()).isEqualTo(403);
	}
	@Test void avaliacaoNovaUsaOPadraoEEdicaoMantemOQueGravou() throws Exception {
		var a=cadastrarNutri();var p=paciente(a);
		requisitar("PUT","/eu/nutri/protocolos",a.token(),Map.of("dobras","TRES","massa","MUSCULAR"),se(0));
		var criada=requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),avaliacao());assertThat(criada.statusCode()).as(criada.body()).isEqualTo(201);
		var av=json(criada);assertThat(av.path("protocolo").path("dobras").asString()).isEqualTo("TRES");assertThat(av.path("protocolo").path("massa").asString()).isEqualTo("MUSCULAR");
		var explicita=json(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),new HashMap<>(avaliacao()){{put("protocolo",Map.of("dobras","SETE","massa","MAGRO"));}}));
		assertThat(explicita.path("protocolo").path("dobras").asString()).isEqualTo("SETE");
		// a nutri muda o padrão; editar a avaliação sem mandar protocolo não muda o resultado (RN-CAL-03)
		requisitar("PUT","/eu/nutri/protocolos",a.token(),Map.of("dobras","SETE","massa","MAGRO"),se(1));
		var corpo=new HashMap<>(avaliacao());corpo.put("parecer","Revisado");
		var editada=requisitar("PUT","/avaliacoes/"+av.path("id").asString(),a.token(),corpo,Map.of("If-Match","\""+av.path("versao").asLong()+"\""));
		assertThat(editada.statusCode()).as(editada.body()).isEqualTo(200);
		assertThat(json(editada).path("protocolo").path("dobras").asString()).isEqualTo("TRES");
		assertThat(json(editada).path("resultados").path("percentualGordura").decimalValue()).isEqualByComparingTo(av.path("resultados").path("percentualGordura").decimalValue());
	}
	@Test void entraNoSyncSoParaANutriDona() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();
		requisitar("PUT","/eu/nutri/protocolos",a.token(),Map.of("dobras","TRES","massa","MAGRO"),se(0));
		var deA=json(requisitar("GET","/sync?limite=1000",a.token(),null)).path("mudancas").path("protocolos");
		assertThat(deA).hasSize(1);assertThat(deA.get(0).path("dados").path("dobras").asString()).isEqualTo("TRES");
		assertThat(json(requisitar("GET","/sync?limite=1000",b.token(),null)).path("mudancas").path("protocolos")).isEmpty();
	}
}
