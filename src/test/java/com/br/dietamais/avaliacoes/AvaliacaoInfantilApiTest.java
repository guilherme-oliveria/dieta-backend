package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class AvaliacaoInfantilApiTest extends TesteComPaciente {
	private static final ZoneId FUSO=ZoneId.of("America/Sao_Paulo");
	private static final Instant DIA=LocalDate.now(FUSO).minusDays(1).atTime(12,0).atZone(FUSO).toInstant();
	private PacienteTeste paciente(ContaTeste nutri,int meses) throws Exception {
		var p=pacienteVinculado(nutri);
		jdbc.update("update ficha_paciente set sexo='MASCULINO',data_nascimento=? where vinculo_id=?::uuid",java.sql.Date.valueOf(DIA.atZone(FUSO).toLocalDate().minusMonths(meses)),p.vinculoId());return p;
	}
	private Map<String,Object> corpo(Map<String,Object> extra){
		var r=new HashMap<String,Object>();r.put("dataHora",DIA.toString());r.put("pesoKg",3.25);r.put("estaturaM",0.485);r.putAll(extra);return r;
	}
	private JsonNode criar(ContaTeste n,PacienteTeste p,Map<String,Object> corpo) throws Exception {
		var r=requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",n.token(),corpo);assertThat(r.statusCode()).as(r.body()).isEqualTo(201);return json(r);
	}
	@Test void salvaLactenteSemArredondarEstaturaELeSnapshotSemRecalcular() throws Exception {
		var n=cadastrarNutri();var p=paciente(n,1);
		var a=criar(n,p,corpo(Map.of("infantil",Map.of("pesoNascerGramas",2500,"modoCurva","ESCORE_Z"))));
		assertThat(a.path("estaturaM").decimalValue()).isEqualByComparingTo("0.485");
		var r=a.path("resultadosInfantis");assertThat(r.path("idadeMeses").asInt()).isEqualTo(1);assertThat(r.path("classificacaoPesoNascer").asString()).isEqualTo("BAIXO");
		assertThat(r.path("curvas")).hasSize(3);assertThat(r.path("curvas").get(0).path("cortesPercentis")).hasSize(8);
		jdbc.update("update ficha_paciente set data_nascimento='2020-01-01' where vinculo_id=?::uuid",p.vinculoId());
		var lido=json(requisitar("GET","/avaliacoes/"+a.path("id").asString(),n.token(),null));
		assertThat(lido.path("resultadosInfantis")).isEqualTo(r);
	}
	@Test void edicaoLegadaPreservaEntradasInfantisERecalculaResultados() throws Exception {
		var n=cadastrarNutri();var p=paciente(n,1);var a=criar(n,p,corpo(Map.of("infantil",Map.of("pesoNascerGramas",3200))));
		var alterado=corpo(Map.of("pesoKg",4));
		var resposta=requisitar("PUT","/avaliacoes/"+a.path("id").asString(),n.token(),alterado,Map.of("If-Match","\""+a.path("versao").asLong()+"\""));
		assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(200);
		var nova=json(resposta);assertThat(nova.path("infantil").path("pesoNascerGramas").asInt()).isEqualTo(3200);
		assertThat(nova.path("resultadosInfantis").path("classificacaoPesoNascer").asString()).isEqualTo("NORMAL");
		assertThat(nova.path("resultadosInfantis").path("energia").path("getRdiKcal").decimalValue()).isEqualByComparingTo("431");
	}
	@Test void pacienteRecebeCurvasMasNaoEntradasOuSecoesOcultasEOutraNutriNaoAcessa() throws Exception {
		var n=cadastrarNutri();var p=paciente(n,156);var outra=cadastrarNutri();
		var a=criar(n,p,corpo(Map.of("pesoKg",40,"estaturaM",1.5,"dobras",Map.of("triceps",10,"subescapular",10),
			"infantil",Map.of("etniaSlaughter","BRANCA","fatorAtividade",1.2),"compartilhamento",Map.of("medidas",false,"composicao",false,"necessidades",false))));
		var respostaPaciente=requisitar("GET","/eu/avaliacoes",p.token(),null);assertThat(respostaPaciente.statusCode()).as(respostaPaciente.body()).isEqualTo(200);
		var itens=json(respostaPaciente).path("itens");assertThat(itens).hasSize(1);var visto=itens.get(0);
		assertThat(visto.has("infantil")).isFalse();var r=visto.path("resultadosInfantis");assertThat(r.path("curvas")).hasSize(1);
		assertThat(r.has("frisancho")).isFalse();assertThat(r.has("composicao")).isFalse();assertThat(r.has("energia")).isFalse();assertThat(r.has("motivosNulos")).isFalse();
		assertThat(requisitar("GET","/avaliacoes/"+a.path("id").asString(),outra.token(),null).statusCode()).isEqualTo(404);
	}
	@Test void g16NaoGeraClassificacaoEAdultoNaoUsaLimiteInfantilPeloCorpo() throws Exception {
		var n=cadastrarNutri();var p=paciente(n,48);
		var a=criar(n,p,corpo(Map.of("pesoKg",16,"estaturaM",1.0,"perimetros",Map.of("braco",16),"dobras",Map.of("triceps",10))));
		var braco=a.path("resultadosInfantis").path("frisancho").get(3);
		assertThat(braco.path("indicador").asString()).isEqualTo("AMBC");assertThat(braco.path("motivo").asString()).isEqualTo("REFERENCIA_INCONSISTENTE_G16");assertThat(braco.has("classificacao")).isFalse();
		var adulto=paciente(n,360);var rejeitado=requisitar("POST","/pacientes/"+adulto.fichaId()+"/avaliacoes",n.token(),corpo(Map.of("infantil",Map.of("pesoNascerGramas",3000))));
		assertThat(rejeitado.statusCode()).as(rejeitado.body()).isEqualTo(422);
	}
	@Test void syncUsaMesmaGravacaoEFiltraNecessidadesDoPaciente() throws Exception {
		var n=cadastrarNutri();var p=paciente(n,1);
		long cursor=jdbc.queryForObject("select last_value from seq_sync",Long.class);
		String id=jdbc.queryForObject("select uuidv7()",String.class);
		var dados=corpo(Map.of("pacienteId",p.fichaId(),"infantil",Map.of("pesoNascerGramas",3200),"compartilhamento",Map.of("necessidades",false)));
		var mutacao=Map.of("idMutacao",UUID.randomUUID().toString(),"entidade","avaliacoes","operacao","CRIAR","id",id,"dados",dados);
		var resposta=requisitar("POST","/sync",n.token(),Map.of("mutacoes",List.of(mutacao)));assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(200);
		assertThat(json(resposta).path("resultados").get(0).path("status").asString()).as(resposta.body()).isEqualTo("APLICADA");
		var leitura=requisitar("GET","/sync?cursor="+cursor,p.token(),null);assertThat(leitura.statusCode()).as(leitura.body()).isEqualTo(200);
		var avaliacoes=json(leitura).path("mudancas").path("avaliacoes");assertThat(avaliacoes).as(leitura.body()).hasSize(1);
		var visto=avaliacoes.get(0).path("dados");assertThat(visto.has("infantil")).isFalse();
		assertThat(visto.path("resultadosInfantis").path("curvas")).hasSize(3);assertThat(visto.path("resultadosInfantis").has("energia")).isFalse();
	}
}
