package com.br.dietamais.contas;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.util.*;
import org.junit.jupiter.api.Test;

class DispositivosApiTest extends TesteComPaciente {
	Map<String,Object> disp(String token){return Map.of("token",token,"plataforma","ANDROID","nomeAparelho","Pixel");}
	@Test void dispositivosSaoDoUsuarioEMudamDeDonoPorToken() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var t="fcm-"+UUID.randomUUID();
		var r1=requisitar("POST","/eu/dispositivos",p.token(),disp(t));assertThat(r1.statusCode()).as(r1.body()).isEqualTo(201);
		assertThat(r1.body()).doesNotContain(t);var id=json(r1).path("id").asString();
		var r2=json(requisitar("POST","/eu/dispositivos",p.token(),disp(t)));assertThat(r2.path("id").asString()).isEqualTo(id);
		assertThat(jdbc.queryForObject("select count(*) from dispositivo where token=?",Integer.class,t)).isEqualTo(1);
		erro(requisitar("DELETE","/eu/dispositivos/"+id,a.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		// o mesmo aparelho entra com outra conta: o token passa para ela
		requisitar("POST","/eu/dispositivos",a.token(),disp(t));
		assertThat(jdbc.queryForObject("select count(*) from dispositivo d join paciente x on x.usuario_id=d.usuario_id where d.token=?",Integer.class,t)).isZero();
		erro(requisitar("POST","/eu/dispositivos",p.token(),Map.of("token","","plataforma","IOS")),400,"OBRIGATORIO");
		assertThat(requisitar("POST","/eu/dispositivos",null,disp(t)).statusCode()).isEqualTo(401);
		var proprio=json(requisitar("POST","/eu/dispositivos",p.token(),disp("outro-"+UUID.randomUUID()))).path("id").asString();
		assertThat(requisitar("DELETE","/eu/dispositivos/"+proprio,p.token(),null).statusCode()).isEqualTo(204);
		for(int i=0;i<12;i++)requisitar("POST","/eu/dispositivos",p.token(),disp("lote-"+i+UUID.randomUUID()));
		assertThat(jdbc.queryForObject("select count(*) from dispositivo d join paciente x on x.usuario_id=d.usuario_id where x.id=?::uuid",Integer.class,p.fichaId())).isLessThanOrEqualTo(10);
	}
	@Test void preferenciasTemPadraoVersaoEIfMatchPorPerfil() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);
		var ini=requisitar("GET","/eu/preferencias-notificacao",p.token(),null);
		assertThat(ini.headers().firstValue("ETag").orElse("")).isEqualTo("\"0\"");
		var padrao=json(ini);assertThat(padrao.path("tipos")).hasSize(3);assertThat(padrao.path("silencio").path("inicio").asString()).startsWith("22:00");
		for(var t:padrao.path("tipos"))assertThat(t.path("ativo").asBoolean()).isTrue();
		var corpo=Map.of("tipos",List.of(Map.of("tipo","CONSULTA_AMANHA","ativo",false)),"silencio",Map.of("ativo",true,"inicio","23:00","fim","06:30"));
		erro(requisitar("PUT","/eu/preferencias-notificacao",p.token(),corpo),400,"FORMATO_INVALIDO");
		var ok=requisitar("PUT","/eu/preferencias-notificacao",p.token(),corpo,Map.of("If-Match","\"0\""));assertThat(ok.statusCode()).as(ok.body()).isEqualTo(200);
		assertThat(ok.headers().firstValue("ETag").orElse("")).isEqualTo("\"1\"");
		var lido=json(requisitar("GET","/eu/preferencias-notificacao",p.token(),null));
		assertThat(lido.path("silencio").path("inicio").asString()).startsWith("23:00");
		for(var t:lido.path("tipos"))assertThat(t.path("ativo").asBoolean()).isEqualTo(!t.path("tipo").asString().equals("CONSULTA_AMANHA"));
		var conflito=requisitar("PUT","/eu/preferencias-notificacao",p.token(),corpo,Map.of("If-Match","\"0\""));erro(conflito,409,"CONFLITO_VERSAO");
		assertThat(json(conflito).path("atual").path("versao").asLong()).isEqualTo(1);
		var tipoDeNutri=Map.of("tipos",List.of(Map.of("tipo","ASSINATURA","ativo",false)),"silencio",Map.of("ativo",false,"inicio","22:00","fim","07:00"));
		erro(requisitar("PUT","/eu/preferencias-notificacao",p.token(),tipoDeNutri,Map.of("If-Match","\"1\"")),400,"FORMATO_INVALIDO");
		var repetido=Map.of("tipos",List.of(Map.of("tipo","NOVA_DIETA","ativo",false),Map.of("tipo","NOVA_DIETA","ativo",true)),"silencio",Map.of("ativo",false,"inicio","22:00","fim","07:00"));
		erro(requisitar("PUT","/eu/preferencias-notificacao",p.token(),repetido,Map.of("If-Match","\"1\"")),400,"FORMATO_INVALIDO");
		var nutri=json(requisitar("GET","/eu/preferencias-notificacao",a.token(),null));assertThat(nutri.path("tipos")).hasSize(3);assertThat(nutri.path("versao").asLong()).isZero();
	}
}
