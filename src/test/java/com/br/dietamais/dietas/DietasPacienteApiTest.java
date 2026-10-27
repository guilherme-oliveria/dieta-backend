package com.br.dietamais.dietas;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class DietasPacienteApiTest extends TesteComPaciente {
	@Test void pacienteVeSoSnapshotsDoProprioVinculoEHistoricoSobreAssinaturaExpirada() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var p=pacienteVinculado(a);var outro=pacienteVinculado(a);var ali=alimentos(a.token(),1);
		var vazio=requisitar("GET","/eu/dietas",p.token(),null);assertThat(vazio.statusCode()).isEqualTo(200);assertThat(json(vazio).path("itens")).isEmpty();
		assertThat(json(vazio).path("dietaEmUsoId").isNull()).isTrue();assertThat(vazio.headers().firstValue("ETag").orElseThrow()).isEqualTo("\"0\"");
		var d1=criarDieta(a.token(),p.fichaId(),dieta("Primeira",refeicao("R",0,item(ali.get(0),"1"))));var rascunho=criarDieta(a.token(),p.fichaId(),dieta("Rascunho",refeicao("R",0,item(ali.get(0),"1"))));
		var s1=publicar(a.token(),d1);criarDieta(a.token(),outro.fichaId(),dieta("Do outro",refeicao("R",0,item(ali.get(0),"1"))));
		var lista=json(requisitar("GET","/eu/dietas",p.token(),null));
		assertThat(lista.path("itens")).hasSize(1);assertThat(lista.path("itens").get(0)).isEqualTo(s1);assertThat(lista.path("dietaEmUsoId").asString()).isEqualTo(d1.path("id").asString());
		assertThat(lista.toString()).doesNotContain(rascunho.path("id").asString());
		// RN-ASS-05: assinatura da nutri expirada não afeta a leitura do paciente
		jdbc.update("update assinatura set status='CANCELADA' where organizacao_id=?",a.organizacao());
		assertThat(json(requisitar("GET","/eu/dietas",p.token(),null)).path("itens")).hasSize(1);
		// perfil errado e sem token
		erro(requisitar("GET","/eu/dietas",a.token(),null),403,"PERFIL_INVALIDO");
		assertThat(requisitar("GET","/eu/dietas",null,null).statusCode()).isEqualTo(401);
		assertThat(json(requisitar("GET","/eu/dietas",outro.token(),null)).path("itens")).isEmpty();
		assertThat(b.nutri()).isNotNull();
	}
	@Test void dietaEmUsoPadraoEhAPublicadaMaisRecenteEPacienteEscolheEntreNutris() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var pa=pacienteVinculado(a);var ali=alimentos(a.token(),1);var alib=alimentos(b.token(),1);
		// segunda nutri convida o mesmo paciente (conta global)
		String codigo=json(requisitar("POST","/pacientes/"+json(requisitar("POST","/pacientes",b.token(),Map.of("nome","Paciente fictício","email",pa.email()))).path("id").asString()+"/convite",b.token(),null)).path("codigo").asString();
		var corpo=new HashMap<String,Object>();corpo.put("aceitouTermo",true);corpo.put("versaoTermo","clinico-teste-v1");corpo.put("dispositivo",Map.of("id",UUID.randomUUID(),"plataforma","ANDROID","nomeAparelho","Aparelho fictício"));
		var aceite=requisitar("POST","/convites/"+codigo+"/aceitar",pa.token(),corpo);assertThat(aceite.statusCode()).as(aceite.body()).isEqualTo(200);
		String fichaB=json(requisitar("GET","/pacientes?busca=",b.token(),null)).path("itens").get(0).path("id").asString();
		var da=publicar(a.token(),criarDieta(a.token(),pa.fichaId(),dieta("A",refeicao("R",0,item(ali.get(0),"1")))));
		var db=publicar(b.token(),criarDieta(b.token(),fichaB,dieta("B",refeicao("R",0,item(alib.get(0),"1")))));
		var lista=json(requisitar("GET","/eu/dietas",pa.token(),null));assertThat(lista.path("itens")).hasSize(2);
		assertThat(lista.path("dietaEmUsoId").asString()).isEqualTo(db.path("dietaId").asString());
		var escolha=requisitar("PUT","/eu/dieta-em-uso",pa.token(),Map.of("dietaId",da.path("dietaId").asString()),Map.of("If-Match","\"0\""));
		assertThat(escolha.statusCode()).as(escolha.body()).isEqualTo(200);assertThat(json(escolha).path("versao").asLong()).isEqualTo(1);assertThat(escolha.headers().firstValue("ETag").orElseThrow()).isEqualTo("\"1\"");
		assertThat(json(requisitar("GET","/eu/dietas",pa.token(),null)).path("dietaEmUsoId").asString()).isEqualTo(da.path("dietaId").asString());
		var conflito=requisitar("PUT","/eu/dieta-em-uso",pa.token(),Map.of("dietaId",db.path("dietaId").asString()),Map.of("If-Match","\"0\""));erro(conflito,409,"CONFLITO_VERSAO");
		assertThat(json(conflito).path("atual").path("dietaId").asString()).isEqualTo(da.path("dietaId").asString());
		// rascunho, dieta inexistente e dieta de outro paciente são 404
		var rasc=criarDieta(a.token(),pa.fichaId(),dieta("Rasc",refeicao("R",0,item(ali.get(0),"1"))));
		erro(requisitar("PUT","/eu/dieta-em-uso",pa.token(),Map.of("dietaId",rasc.path("id").asString()),Map.of("If-Match","\"1\"")),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("PUT","/eu/dieta-em-uso",pa.token(),Map.of("dietaId",UUID.randomUUID().toString()),Map.of("If-Match","\"1\"")),404,"RECURSO_NAO_ENCONTRADO");
		var estranho=pacienteVinculado(a);var estranha=publicar(a.token(),criarDieta(a.token(),estranho.fichaId(),dieta("Estranha",refeicao("R",0,item(ali.get(0),"1")))));
		erro(requisitar("PUT","/eu/dieta-em-uso",pa.token(),Map.of("dietaId",estranha.path("dietaId").asString()),Map.of("If-Match","\"1\"")),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("PUT","/eu/dieta-em-uso",a.token(),Map.of("dietaId",da.path("dietaId").asString()),Map.of("If-Match","\"1\"")),403,"PERFIL_INVALIDO");
		// nova publicação da mesma nutri: a escolha segue a sucessora (mesmo vínculo)
		var nova=publicar(a.token(),criarDieta(a.token(),pa.fichaId(),dieta("A2",refeicao("R",0,item(ali.get(0),"2")))));
		assertThat(json(requisitar("GET","/eu/dietas",pa.token(),null)).path("dietaEmUsoId").asString()).isEqualTo(nova.path("dietaId").asString());
		// encerrar o vínculo com a nutri A tira a dieta A da candidatura; histórico segue legível
		assertThat(requisitar("POST","/pacientes/"+pa.fichaId()+"/encerrar-vinculo",a.token(),null).statusCode()).isEqualTo(200);
		var fim=json(requisitar("GET","/eu/dietas",pa.token(),null));assertThat(fim.path("dietaEmUsoId").asString()).isEqualTo(db.path("dietaId").asString());assertThat(fim.path("itens").size()).isGreaterThanOrEqualTo(3);
	}
}
