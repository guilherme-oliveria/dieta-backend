package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** BE-56b, RN-EXA-01: exames lançados por avaliação, classificados no servidor; o paciente só vê se a nutri compartilhar. */
class AvaliacaoExamesApiTest extends TesteComPaciente {
	Map<String,Object> corpo(List<Map<String,Object>> exames,Map<String,Object> extra){
		var m=new HashMap<String,Object>();m.put("dataHora",Instant.now().minusSeconds(60).toString());m.put("pesoKg",80);m.put("estaturaM",1.80);
		if(exames!=null)m.put("exames",exames);m.putAll(extra);return m;
	}
	PacienteTeste paciente(ContaTeste nutri,String sexo,LocalDate nascimento) throws Exception {
		var p=pacienteVinculado(nutri);
		jdbc.update("update ficha_paciente set sexo=?,data_nascimento=? where vinculo_id=?::uuid",sexo,nascimento==null?null:java.sql.Date.valueOf(nascimento),p.vinculoId());
		return p;
	}
	JsonNode criar(ContaTeste nutri,PacienteTeste p,Map<String,Object> corpo) throws Exception {
		var r=requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",nutri.token(),corpo);assertThat(r.statusCode()).as(r.body()).isEqualTo(201);return json(r);
	}
	static Map<String,Object> exame(String codigo,Object valor){return Map.of("codigo",codigo,"valor",valor);}

	@Test void lancaExamesEClassificaNoServidor() throws Exception {
		var a=cadastrarNutri();var p=paciente(a,"MASCULINO",LocalDate.now().minusYears(30));
		var av=criar(a,p,corpo(List.of(exame("glicemia-de-jejum",126),exame("acido-urico",4)),Map.of()));
		var exames=av.path("exames");assertThat(exames).hasSize(2);
		assertThat(exames.get(0).path("rotulo").asString()).isEqualTo("diabetes");assertThat(exames.get(0).path("nome").asString()).isNotBlank();assertThat(exames.get(0).path("unidade").asString()).isNotBlank();
		assertThat(exames.get(1).path("rotulo").asString()).isEqualTo("normal");
		var lido=json(requisitar("GET","/avaliacoes/"+av.path("id").asString(),a.token(),null));
		assertThat(lido.path("exames")).hasSize(2);
	}
	@Test void exameQueDependeDeFaseSemCondicaoVemComMotivo() throws Exception {
		var a=cadastrarNutri();var p=paciente(a,"FEMININO",LocalDate.now().minusYears(30));
		var av=criar(a,p,corpo(List.of(exame("ferritina",50)),Map.of()));
		var e=av.path("exames").get(0);assertThat(e.has("rotulo")).isFalse();assertThat(e.path("motivo").asString()).isEqualTo("FALTA_CONDICAO");
		var com=criar(a,p,corpo(List.of(Map.of("codigo","ferritina","valor",50,"condicao","menarca")),Map.of()));
		assertThat(com.path("exames").get(0).path("motivo").isMissingNode()).isTrue();
	}
	@Test void rejeitaExameInexistenteRepetidoOuComValorErrado() throws Exception {
		var a=cadastrarNutri();var p=paciente(a,"MASCULINO",LocalDate.now().minusYears(30));
		for(var ruim:List.of(List.of(exame("nao-existe",1)),List.of(exame("glicemia-de-jejum",90),exame("glicemia-de-jejum",91)),List.of(Map.<String,Object>of("codigo","glicemia-de-jejum","texto","normal")),
				List.of(Map.<String,Object>of("codigo","glicemia-de-jejum","valor",90,"condicao","inventada")))){
			erro(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),corpo(ruim,Map.of())),400,"FORMATO_INVALIDO");
		}
	}
	@Test void semNascimentoNaoLancaExame() throws Exception {
		var a=cadastrarNutri();var p=paciente(a,"MASCULINO",null);
		erro(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),corpo(List.of(exame("glicemia-de-jejum",90)),Map.of())),400,"FORMATO_INVALIDO");
	}
	@Test void edicaoSemListaMantemEListaVaziaLimpa() throws Exception {
		var a=cadastrarNutri();var p=paciente(a,"MASCULINO",LocalDate.now().minusYears(30));
		var av=criar(a,p,corpo(List.of(exame("glicemia-de-jejum",90)),Map.of()));String id=av.path("id").asString();
		var mantida=requisitar("PUT","/avaliacoes/"+id,a.token(),corpo(null,Map.of("pesoKg",79)),Map.of("If-Match","\"1\""));assertThat(mantida.statusCode()).as(mantida.body()).isEqualTo(200);
		assertThat(json(mantida).path("exames")).hasSize(1);
		var limpa=requisitar("PUT","/avaliacoes/"+id,a.token(),corpo(List.of(),Map.of()),Map.of("If-Match","\"2\""));assertThat(limpa.statusCode()).as(limpa.body()).isEqualTo(200);
		assertThat(json(limpa).has("exames")).isFalse();
	}
	@Test void pacienteSoVeExamesQuandoACompartilha() throws Exception {
		var a=cadastrarNutri();var p=paciente(a,"MASCULINO",LocalDate.now().minusYears(30));var b=cadastrarNutri();
		var padrao=criar(a,p,corpo(List.of(exame("glicemia-de-jejum",90)),Map.of()));
		var meus=json(requisitar("GET","/eu/avaliacoes",p.token(),null)).path("itens");assertThat(meus).hasSize(1);assertThat(meus.get(0).has("exames")).isFalse();
		jdbc.update("update avaliacao set compartilhamento='{\"exames\":true}'::jsonb where id=?",UUID.fromString(padrao.path("id").asString()));
		var vistos=json(requisitar("GET","/eu/avaliacoes",p.token(),null)).path("itens");assertThat(vistos.get(0).path("exames")).hasSize(1);
		assertThat(requisitar("GET","/avaliacoes/"+padrao.path("id").asString(),b.token(),null).statusCode()).isEqualTo(404);
	}
}
