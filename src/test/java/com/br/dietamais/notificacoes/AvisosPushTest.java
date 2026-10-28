package com.br.dietamais.notificacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.TestcontainersConfiguration;
import com.br.dietamais.agenda.aplicacao.LembretesConsulta;
import com.br.dietamais.comum.api.Perfil;
import com.br.dietamais.dietas.TesteComPaciente;
import com.br.dietamais.notificacoes.api.*;
import com.br.dietamais.notificacoes.aplicacao.ProcessarPush;
import com.br.dietamais.rotina.aplicacao.AlertaSemCheckin;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Import({TestcontainersConfiguration.class,PushTesteConfig.class})
@TestPropertySource(properties={"dieta.push.habilitado=true","dieta.push.intervalo-envio=PT24H","dieta.avisos.intervalo-consultas=PT24H","dieta.avisos.intervalo-sem-checkin=PT24H"})
class AvisosPushTest extends TesteComPaciente {
	static final ZoneId FUSO=ZoneId.of("America/Sao_Paulo");
	@Autowired ProcessarPush push;@Autowired PushTesteConfig.EnviadorFalso falso;@Autowired RegistrarAviso avisos;
	@Autowired LembretesConsulta lembretes;@Autowired AlertaSemCheckin semCheckin;

	@BeforeEach void limpar(){falso.enviadas.clear();falso.resultados.clear();while(push.processarProximo()){}}

	void prefs(PacienteTeste p,boolean silencio,String desligados){
		jdbc.update("insert into preferencia_notificacao(usuario_id,silencio_ativo,tipos_desligados) values(?::uuid,?,?::jsonb) on conflict(usuario_id) do update set silencio_ativo=excluded.silencio_ativo,tipos_desligados=excluded.tipos_desligados",p.usuarioId(),silencio,desligados);
	}
	String dispositivo(PacienteTeste p) throws Exception {
		String t="fcm-"+UUID.randomUUID();
		assertThat(requisitar("POST","/eu/dispositivos",p.token(),Map.of("token",t,"plataforma","ANDROID","nomeAparelho","Pixel")).statusCode()).isEqualTo(201);return t;
	}
	void publicarDieta(ContaTeste a,PacienteTeste p) throws Exception {
		publicar(a.token(),criarDieta(a.token(),p.fichaId(),dieta("Aviso",refeicao("Café",0,item(alimentos(a.token(),1).get(0),"1")))));
	}
	String status(PacienteTeste p,String tipo){return jdbc.queryForObject("select push_status from aviso where usuario_id=?::uuid and tipo=?",String.class,p.usuarioId(),tipo);}

	@Test void novaDietaViraAvisoEPushGenerico() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);prefs(p,false,"[]");var t=dispositivo(p);
		publicarDieta(a,p);
		assertThat(status(p,"NOVA_DIETA")).isEqualTo("PENDENTE");
		while(push.processarProximo()){}
		assertThat(status(p,"NOVA_DIETA")).isEqualTo("ENVIADO");
		var m=falso.enviadas.stream().filter(x->x.token().equals(t)).findFirst().orElseThrow();
		assertThat(m.tipo()).isEqualTo("NOVA_DIETA");assertThat(m.rota()).isEqualTo("/dieta");
		assertThat(m.titulo()+m.corpo()).doesNotContainIgnoringCase("kcal").doesNotContain("Aviso");
		// a nutri não recebe aviso de nova dieta
		assertThat(jdbc.queryForObject("select count(*) from aviso where usuario_id=?",Integer.class,a.usuario())).isZero();
	}

	@Test void tipoDesligadoFicaNoHistoricoMasNaoVaiParaOAparelho() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);prefs(p,false,"[\"NOVA_DIETA\"]");dispositivo(p);
		publicarDieta(a,p);while(push.processarProximo()){}
		assertThat(status(p,"NOVA_DIETA")).isEqualTo("DESLIGADO");assertThat(falso.enviadas).isEmpty();
	}

	@Test void silencioAdiaParaOFimDaJanela() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);prefs(p,true,"[]");dispositivo(p);
		var agora=LocalTime.now(FUSO).truncatedTo(ChronoUnit.MINUTES);
		jdbc.update("update preferencia_notificacao set silencio_inicio=?,silencio_fim=? where usuario_id=?::uuid",java.sql.Time.valueOf(agora.minusHours(1)),java.sql.Time.valueOf(agora.plusHours(1)),p.usuarioId());
		publicarDieta(a,p);while(push.processarProximo()){}
		assertThat(status(p,"NOVA_DIETA")).isEqualTo("PENDENTE");assertThat(falso.enviadas).isEmpty();
		var proxima=jdbc.queryForObject("select push_proxima_em from aviso where usuario_id=?::uuid",java.sql.Timestamp.class,p.usuarioId()).toInstant();
		assertThat(proxima).isAfter(Instant.now().plus(30,ChronoUnit.MINUTES)).isBefore(Instant.now().plus(130,ChronoUnit.MINUTES));
		// o silêncio acaba: o aviso sai
		jdbc.update("update preferencia_notificacao set silencio_ativo=false where usuario_id=?::uuid",p.usuarioId());
		jdbc.update("update aviso set push_proxima_em=now() where usuario_id=?::uuid",p.usuarioId());
		while(push.processarProximo()){}
		assertThat(status(p,"NOVA_DIETA")).isEqualTo("ENVIADO");
	}

	@Test void semAparelhoTokenInvalidoEFalhaTemporaria() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);prefs(p,false,"[]");
		publicarDieta(a,p);while(push.processarProximo()){}
		assertThat(status(p,"NOVA_DIETA")).isEqualTo("SEM_DISPOSITIVO");
		// token recusado pelo FCM é apagado
		var b=cadastrarNutri();var q=pacienteVinculado(b);prefs(q,false,"[]");var ruim=dispositivo(q);falso.resultados.put(ruim,ResultadoPush.TOKEN_INVALIDO);
		publicarDieta(b,q);while(push.processarProximo()){}
		assertThat(status(q,"NOVA_DIETA")).isEqualTo("SEM_DISPOSITIVO");
		assertThat(jdbc.queryForObject("select count(*) from dispositivo where token=?",Integer.class,ruim)).isZero();
		// falha temporária repete com recuo e desiste na 5ª
		var c=cadastrarNutri();var r=pacienteVinculado(c);prefs(r,false,"[]");var lento=dispositivo(r);falso.resultados.put(lento,ResultadoPush.FALHA_TEMPORARIA);
		publicarDieta(c,r);
		for(int i=1;i<=5;i++){
			while(push.processarProximo()){}
			if(i<5){
				assertThat(status(r,"NOVA_DIETA")).isEqualTo("PENDENTE");
				assertThat(jdbc.queryForObject("select push_tentativas from aviso where usuario_id=?::uuid",Integer.class,r.usuarioId())).isEqualTo(i);
				jdbc.update("update aviso set push_proxima_em=now() where usuario_id=?::uuid",r.usuarioId());
			}
		}
		assertThat(status(r,"NOVA_DIETA")).isEqualTo("FALHA");
		assertThat(jdbc.queryForObject("select count(*) from dispositivo where token=?",Integer.class,lento)).isOne();
	}

	@Test void avisoVelhoExpiraEDeduplicaPelaChave() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);prefs(p,false,"[]");dispositivo(p);
		var aviso=new NovoAviso(Perfil.PACIENTE,UUID.fromString(p.fichaId()),TipoAviso.QUESTIONARIO_ENVIADO,"/questionarios/x","chave-"+p.fichaId());
		avisos.registrar(aviso);avisos.registrar(aviso);
		assertThat(jdbc.queryForObject("select count(*) from aviso where usuario_id=?::uuid",Integer.class,p.usuarioId())).isOne();
		jdbc.update("update aviso set criado_em=now()-interval '25 hours' where usuario_id=?::uuid",p.usuarioId());
		while(push.processarProximo()){}
		assertThat(status(p,"QUESTIONARIO_ENVIADO")).isEqualTo("EXPIRADO");assertThat(falso.enviadas).isEmpty();
		// perfil errado para o tipo é recusado sem derrubar o chamador
		avisos.registrar(new NovoAviso(Perfil.NUTRICIONISTA,a.nutri(),TipoAviso.NOVA_DIETA,null,null));
		assertThat(jdbc.queryForObject("select count(*) from aviso where usuario_id=?",Integer.class,a.usuario())).isZero();
	}

	@Test void lembreteDeConsultaVai24hAntesParaOsDoisLados() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);
		var perto=Instant.now().plus(20,ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);var longe=Instant.now().plus(40,ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
		for(var quando:List.of(perto,longe))assertThat(requisitar("POST","/consultas",a.token(),Map.of("pacienteId",p.fichaId(),"inicio",quando.toString(),"duracaoMin",30,"tipo","RETORNO")).statusCode()).isEqualTo(201);
		lembretes.executar();lembretes.executar();
		assertThat(jdbc.queryForObject("select count(*) from aviso where tipo='CONSULTA_AMANHA' and usuario_id in (?::uuid,?)",Integer.class,p.usuarioId(),a.usuario())).isEqualTo(2);
		var aviso=jdbc.queryForMap("select titulo,corpo,rota from aviso where tipo='CONSULTA_AMANHA' and usuario_id=?::uuid",p.usuarioId());
		assertThat(aviso.get("rota")).isEqualTo("/agenda");assertThat(aviso.get("corpo").toString()).doesNotContainPattern("\\d{1,2}:\\d{2}");
		// cancelada não avisa
		var outra=cadastrarNutri();var q=pacienteVinculado(outra);
		var criada=json(requisitar("POST","/consultas",outra.token(),Map.of("pacienteId",q.fichaId(),"inicio",perto.toString(),"duracaoMin",30,"tipo","RETORNO")));
		requisitar("PATCH","/consultas/"+criada.path("id").asString(),outra.token(),Map.of("status","CANCELADA"),Map.of("If-Match","\"1\""));
		lembretes.executar();
		assertThat(jdbc.queryForObject("select count(*) from aviso where usuario_id=?::uuid",Integer.class,q.usuarioId())).isZero();
	}

	@Test void pacienteHaTresDiasSemCheckinAvisaANutriUmaVez() throws Exception {
		var a=cadastrarNutri();var parado=pacienteVinculado(a);var ativo=pacienteVinculado(a);
		for(var p:List.of(parado,ativo)){
			publicarDieta(a,p);
			jdbc.update("insert into dieta_em_uso(paciente_id,dieta_id) select ?::uuid,(select id from dieta where paciente_id=?::uuid limit 1) on conflict(paciente_id) do nothing",p.fichaId(),p.fichaId());
		}
		jdbc.update("update dieta_em_uso set atualizado_em=now()-interval '5 days' where paciente_id=?::uuid",parado.fichaId());
		semCheckin.executar();semCheckin.executar();
		var linhas=jdbc.queryForList("select rota from aviso where usuario_id=? and tipo='PACIENTE_SEM_CHECKIN'",String.class,a.usuario());
		assertThat(linhas).containsExactly("/pacientes/"+parado.fichaId());
	}
}
