package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class AvaliacoesApiTest extends TesteComPaciente {
	Map<String,Object> corpo(Object peso,Object estatura){
		var m=new HashMap<String,Object>();m.put("dataHora",Instant.now().minusSeconds(60).toString());m.put("pesoKg",peso);m.put("estaturaM",estatura);return m;
	}
	@Test void criaCalculaImcEValidaLimitesEIsolamento() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var b=cadastrarNutri();
		var r=requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),corpo(70,1.75));
		assertThat(r.statusCode()).as(r.body()).isEqualTo(201);var av=json(r);
		assertThat(av.path("imc").decimalValue()).isEqualByComparingTo("22.86");assertThat(av.path("classificacaoImc").asString()).isEqualTo("NORMAL");
		assertThat(av.path("compartilhada").asBoolean()).isTrue();assertThat(r.headers().firstValue("ETag").orElseThrow()).isEqualTo("\"1\"");
		var c=corpo(70,1.75);c.put("perimetros",Map.of("cintura",9));erro(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),c),422,"VALOR_FORA_DO_INTERVALO");
		c=corpo(70,1.75);c.put("dobras",Map.of("triceps",81));erro(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),c),422,"VALOR_FORA_DO_INTERVALO");
		erro(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),corpo(19.9,1.75)),422,"VALOR_FORA_DO_INTERVALO");
		erro(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),corpo(70,2.6)),422,"VALOR_FORA_DO_INTERVALO");
		var futura=corpo(70,1.75);futura.put("dataHora",Instant.now().plusSeconds(86400).toString());
		erro(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),futura),400,"FORMATO_INVALIDO");
		// isolamento: outra nutri recebe 404 em tudo e nada vaza
		String id=av.path("id").asString();
		erro(requisitar("GET","/avaliacoes/"+id,b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("GET","/pacientes/"+p.fichaId()+"/avaliacoes",b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",b.token(),corpo(70,1.75)),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("PUT","/avaliacoes/"+id,b.token(),corpo(71,1.75),Map.of("If-Match","\"1\"")),404,"RECURSO_NAO_ENCONTRADO");
		assertThat(requisitar("GET","/avaliacoes/"+id,null,null).statusCode()).isEqualTo(401);
		assertThat(json(requisitar("GET","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),null)).path("itens")).hasSize(1);
	}
	@Test void atualizaComIfMatchEExigeJustificativaDepoisDe30Dias() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);
		var av=json(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),corpo(80,1.80)));String id=av.path("id").asString();
		var ok=requisitar("PUT","/avaliacoes/"+id,a.token(),corpo(79,1.80),Map.of("If-Match","\"1\""));
		assertThat(ok.statusCode()).as(ok.body()).isEqualTo(200);assertThat(json(ok).path("versao").asLong()).isEqualTo(2);
		var velho=requisitar("PUT","/avaliacoes/"+id,a.token(),corpo(78,1.80),Map.of("If-Match","\"1\""));erro(velho,409,"CONFLITO_VERSAO");
		assertThat(json(velho).path("atual").path("versao").asLong()).isEqualTo(2);
		jdbc.update("update avaliacao set criado_em=now()-interval '31 days' where id=?",UUID.fromString(id));
		erro(requisitar("PUT","/avaliacoes/"+id,a.token(),corpo(78,1.80),Map.of("If-Match","\"2\"")),422,"JUSTIFICATIVA_OBRIGATORIA");
		var c=corpo(78,1.80);c.put("justificativa","Correção de digitação");
		var r=requisitar("PUT","/avaliacoes/"+id,a.token(),c,Map.of("If-Match","\"2\""));assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
		assertThat(jdbc.queryForObject("select count(*) from avaliacao_justificativa where avaliacao_id=?",Integer.class,UUID.fromString(id))).isEqualTo(1);
	}
	@Test void pacienteVeSoCompartilhadasEOSyncEntregaOuRemove() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);
		var privada=corpo(70,1.70);privada.put("compartilhada",false);
		var rp=requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),privada);assertThat(rp.statusCode()).as(rp.body()).isEqualTo(201);
		var visivel=json(requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),corpo(71,1.70)));
		var lista=json(requisitar("GET","/eu/avaliacoes",p.token(),null)).path("itens");assertThat(lista).hasSize(1);assertThat(lista.get(0).path("id").asString()).isEqualTo(visivel.path("id").asString());
		erro(requisitar("GET","/eu/avaliacoes",a.token(),null),403,"PERFIL_INVALIDO");
		var sync=json(requisitar("GET","/sync?limite=1000",p.token(),null)).path("mudancas").path("avaliacoes");
		assertThat(sync).hasSize(1);assertThat(sync.get(0).path("id").asString()).isEqualTo(visivel.path("id").asString());
		// descompartilhar gera lápide para o paciente
		var cursor=json(requisitar("GET","/sync?limite=1000",p.token(),null)).path("cursor").asString();
		var oculta=corpo(71,1.70);oculta.put("compartilhada",false);
		assertThat(requisitar("PUT","/avaliacoes/"+visivel.path("id").asString(),a.token(),oculta,Map.of("If-Match","\"1\"")).statusCode()).isEqualTo(200);
		var depois=json(requisitar("GET","/sync?limite=1000&cursor="+cursor,p.token(),null)).path("mudancas").path("avaliacoes");
		assertThat(depois).hasSize(1);assertThat(depois.get(0).path("excluido").asBoolean()).isTrue();
		assertThat(json(requisitar("GET","/eu/avaliacoes",p.token(),null)).path("itens")).isEmpty();
	}
}
