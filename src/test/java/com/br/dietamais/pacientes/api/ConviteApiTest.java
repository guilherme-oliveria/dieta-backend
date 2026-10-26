package com.br.dietamais.pacientes.api;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.TesteIsolamento;
import com.br.dietamais.notificacoes.aplicacao.ProcessarEmails;
import com.br.dietamais.TestcontainersConfiguration;
import java.net.http.HttpResponse;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import tools.jackson.databind.JsonNode;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"dieta.auth.login-limite-ip=10000","dieta.convite.limite-ip=10000",
	"dieta.consentimento.versao=clinico-teste-v1","dieta.consentimento.texto=Termo clínico fictício exclusivamente para teste"})
@Import({TestcontainersConfiguration.class,ConviteApiTest.Configuracao.class})
class ConviteApiTest extends TesteIsolamento {
	static final Instant AGORA=Instant.parse("2026-10-26T12:00:00Z");
	static final String CHAVE=chave();
	@DynamicPropertySource static void propriedades(DynamicPropertyRegistry registro){registro.add("IDEMPOTENCIA_CHAVE",()->CHAVE);}
	static String chave(){byte[] chave=new byte[32];new java.security.SecureRandom().nextBytes(chave);return Base64.getEncoder().encodeToString(chave);}
	@Autowired RelogioTeste relogio;
	@Autowired ProcessarEmails emails;
	@Autowired TestcontainersConfiguration.TransporteEmailTeste transporte;
	@AfterEach void resetar(){relogio.agora.set(AGORA);}
	@TestConfiguration(proxyBeanMethods=false) static class Configuracao {@Bean @Primary RelogioTeste relogioConvite(){return new RelogioTeste();}}
	static class RelogioTeste extends Clock {
		final AtomicReference<Instant> agora=new AtomicReference<>(AGORA);
		public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zona){return this;}public Instant instant(){return agora.get();}
	}
	@Override protected ContaTeste cadastrarNutri() throws Exception {
		var conta=super.cadastrarNutri();jdbc.update("update usuario set email_confirmado_em=? where id=?",java.sql.Timestamp.from(AGORA),conta.usuario());return conta;
	}
	@Test void nutriPrecisaConfirmarEmailAntesDeConvidar() throws Exception {
		var a=super.cadastrarNutri();String p=criarFicha(a.token(),null);
		erro(requisitar("POST","/pacientes/"+p+"/convite",a.token(),null),422,"EMAIL_NAO_CONFIRMADO");
		jdbc.update("update usuario set email_confirmado_em=? where id=?",java.sql.Timestamp.from(AGORA),a.usuario());
		assertThat(requisitar("POST","/pacientes/"+p+"/convite",a.token(),null).statusCode()).isEqualTo(201);
	}
	@Test void gerarReenviarConsultarExpirarEEmailNaoRevelamPaciente() throws Exception {
		var nutri=cadastrarNutri();String email=email();String paciente=criarFicha(nutri.token(),email);
		var criado=convidar(nutri.token(),paciente);String antigo=texto(criado,"codigo");
		assertThat(antigo).matches("[A-HJ-NP-Z2-9]{8}");assertThat(texto(criado,"expiraEm")).isEqualTo(AGORA.plus(Duration.ofDays(7)).toString());
		assertThat(texto(criado,"link")).isEqualTo("dietamais://convites/"+antigo);
		var publico=requisitar("GET","/convites/"+antigo,null,null);assertThat(publico.statusCode()).as(publico.body()).isEqualTo(200);
		assertThat(publico.body()).doesNotContain(email,paciente,"Paciente fictício");assertThat(json(publico).path("termo").path("versao").asString()).isEqualTo("clinico-teste-v1");
		String codigo=texto(convidar(nutri.token(),paciente),"codigo");assertThat(codigo).isNotEqualTo(antigo);
		erro(requisitar("GET","/convites/"+antigo,null,null),422,"CONVITE_INVALIDO");
		assertThat(jdbc.queryForList("select hash_codigo from convite where vinculo_id=(select id from vinculo where paciente_id=?)",String.class,UUID.fromString(paciente))).allSatisfy(hash->assertThat(hash).hasSize(64).doesNotContain(antigo,codigo));
		assertThat(jdbc.queryForList("select payload_cifrado from notificacao_pendente where tipo='CONVITE'",String.class)).allSatisfy(payload->assertThat(payload).startsWith("v1.").doesNotContain(email,codigo));
		for(int i=0;i<100&&emails.processarProxima();i++){}
		assertThat(transporte.mensagens).anySatisfy(mensagem->{assertThat(mensagem.destinatario()).isEqualTo(email);assertThat(mensagem.html()).contains("dietamais://convites/").doesNotContain("Paciente fictício");});
		relogio.agora.set(AGORA.plus(Duration.ofDays(7)));erro(requisitar("GET","/convites/"+codigo,null,null),422,"CONVITE_EXPIRADO");
	}
	@Test void aceiteCriaContaArgonConsentimentoESessaoPacienteUmaUnicaVez() throws Exception {
		var nutri=cadastrarNutri();String email=email();String paciente=criarFicha(nutri.token(),email);String codigo=texto(convidar(nutri.token(),paciente),"codigo");
		var aceite=aceitar(codigo,null,email);assertThat(aceite.statusCode()).as(aceite.body()).isEqualTo(200);
		var dados=json(aceite);assertThat(dados.path("vinculo").path("status").asString()).isEqualTo("ATIVO");
		assertThat(dados.path("sessao").path("usuario").path("perfil").asString()).isEqualTo("PACIENTE");
		UUID uid=UUID.fromString(dados.path("sessao").path("usuario").path("id").asString());
		assertThat(jdbc.queryForObject("select senha_hash from usuario where id=?",String.class,uid)).startsWith("$argon2id$");
		assertThat(jdbc.queryForObject("select versao_termos is null and termos_aceitos_em is null from usuario where id=?",Boolean.class,uid)).isTrue();
		var consentimento=jdbc.queryForMap("select * from consentimento where usuario_id=?",uid);
		assertThat(consentimento).containsEntry("versao_termo","clinico-teste-v1");assertThat(consentimento.get("origem_ip")).isNotNull();assertThat(consentimento.get("hash_termo").toString()).hasSize(64);
		String token=dados.path("sessao").path("accessToken").asString();assertThat(requisitar("GET","/eu",token,null).statusCode()).isEqualTo(200);
		erro(aceitar(codigo,null,email),422,"CONVITE_INVALIDO");
	}
	@Test void falsoConsentimentoVersaoErradaOuEmailAlheioNaoConsomemConvite() throws Exception {
		var nutri=cadastrarNutri();String email=email();String paciente=criarFicha(nutri.token(),email);String codigo=texto(convidar(nutri.token(),paciente),"codigo");
		var corpo=corpo(null,email);corpo.put("aceitouTermo",false);erro(requisitar("POST","/convites/"+codigo+"/aceitar",null,corpo),422,"CONSENTIMENTO_OBRIGATORIO");
		corpo=corpo(null,email);corpo.put("versaoTermo","versao-falsa");erro(requisitar("POST","/convites/"+codigo+"/aceitar",null,corpo),422,"CONSENTIMENTO_OBRIGATORIO");
		erro(aceitar(codigo,null,email()),422,"CONVITE_INVALIDO");
		assertThat(aceitar(codigo,null,email).statusCode()).isEqualTo(200);
	}
	@Test void contaExistenteExigeBearerEReutilizaPacienteGlobalSemAlterarOutraFicha() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();String email=email();String pa=criarFicha(a.token(),email);String ca=texto(convidar(a.token(),pa),"codigo");
		var primeiro=json(aceitar(ca,null,email));String token=primeiro.path("sessao").path("accessToken").asString();
		String pb=criarFicha(b.token(),email);String cb=texto(convidar(b.token(),pb),"codigo");
		erro(aceitar(cb,null,email),409,"EMAIL_JA_CADASTRADO");
		var segundo=aceitar(cb,token,email);assertThat(segundo.statusCode()).as(segundo.body()).isEqualTo(200);
		assertThat(json(segundo).path("vinculo").path("pacienteId").asString()).isEqualTo(pa);
		assertThat(jdbc.queryForObject("select paciente_origem_id from vinculo where id=?",UUID.class,UUID.fromString(json(segundo).path("vinculo").path("id").asString()))).isEqualTo(UUID.fromString(pb));
		assertThat(jdbc.queryForObject("select count(*) from paciente where usuario_id=?",Integer.class,UUID.fromString(primeiro.path("sessao").path("usuario").path("id").asString()))).isEqualTo(1);
		assertThat(requisitar("GET","/pacientes/"+pa,a.token(),null).statusCode()).isEqualTo(200);
		assertThat(requisitar("GET","/pacientes/"+pa,b.token(),null).statusCode()).isEqualTo(200);
		erro(requisitar("GET","/pacientes/"+pb,a.token(),null),404,"RECURSO_NAO_ENCONTRADO");
	}
	@Test void usoConcorrenteTemUmSucessoEUmConviteInvalido() throws Exception {
		var nutri=cadastrarNutri();String email=email();String p=criarFicha(nutri.token(),email);String codigo=texto(convidar(nutri.token(),p),"codigo");
		try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
			var resultados=executor.invokeAll(List.<Callable<Integer>>of(()->aceitar(codigo,null,email).statusCode(),()->aceitar(codigo,null,email).statusCode()));
			assertThat(List.of(resultados.get(0).get(),resultados.get(1).get())).containsExactlyInAnyOrder(200,422);
		}
		assertThat(jdbc.queryForObject("select count(*) from consentimento where vinculo_id=(select id from vinculo where paciente_id=?)",Integer.class,UUID.fromString(p))).isEqualTo(1);
	}
	@Test void conviteNoLimiteNaoDuplicaOcupacaoEDonoAlheioRecebe404() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();String ultimo=null;
		for(int i=0;i<5;i++)ultimo=criarFicha(a.token(),null);
		assertThat(requisitar("POST","/pacientes/"+ultimo+"/convite",a.token(),null).statusCode()).isEqualTo(201);
		erro(requisitar("POST","/pacientes/"+ultimo+"/convite",b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		jdbc.update("update plano set limite_pacientes=4 where id=(select plano_id from assinatura where organizacao_id=?)",a.organizacao());
		try{erro(requisitar("POST","/pacientes/"+ultimo+"/convite",a.token(),null),422,"LIMITE_PACIENTES_ATINGIDO");}
		finally{jdbc.update("update plano set limite_pacientes=5 where codigo='TESTE'");}
	}
	@Test void pacienteEscolheVinculoEEncerramentoNaoAfetaOutrasNutris() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();String email=email();String p=criarFicha(a.token(),email);
		var aceite=json(aceitar(texto(convidar(a.token(),p),"codigo"),null,email));String token=aceite.path("sessao").path("accessToken").asString();String va=aceite.path("vinculo").path("id").asString();
		String pb=criarFicha(b.token(),email);var segundo=json(aceitar(texto(convidar(b.token(),pb),"codigo"),token,email));String vb=segundo.path("vinculo").path("id").asString();
		erro(requisitar("POST","/pacientes/"+p+"/encerrar-vinculo",token,null),400,"OBRIGATORIO");
		erro(requisitar("POST","/pacientes/"+p+"/encerrar-vinculo?vinculoId="+vb,a.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		var encerrado=requisitar("POST","/pacientes/"+p+"/encerrar-vinculo?vinculoId="+va,token,null);assertThat(encerrado.statusCode()).as(encerrado.body()).isEqualTo(200);
		assertThat(json(encerrado).path("status").asString()).isEqualTo("ENCERRADO");
		assertThat(jdbc.queryForObject("select status from vinculo where id=?",String.class,UUID.fromString(vb))).isEqualTo("ATIVO");
		erro(requisitar("PATCH","/pacientes/"+p,a.token(),Map.of("nome","Tentativa"),Map.of("If-Match","\"2\"")),403,"ACESSO_NEGADO");
		assertThat(requisitar("GET","/pacientes/"+p,a.token(),null).statusCode()).isEqualTo(200);
	}
	@Test void encerramentoInvalidaConviteELiberaVaga() throws Exception {
		var a=cadastrarNutri();String p=criarFicha(a.token(),null);String codigo=texto(convidar(a.token(),p),"codigo");
		assertThat(requisitar("POST","/pacientes/"+p+"/encerrar-vinculo",a.token(),null).statusCode()).isEqualTo(200);
		erro(requisitar("GET","/convites/"+codigo,null,null),422,"CONVITE_INVALIDO");
		assertThat(json(requisitar("GET","/eu",a.token(),null)).path("assinatura").path("pacientesOcupados").asInt()).isZero();
	}
	@Test void reenvioConcorrenteComAceiteNaoConsomeCodigoInvalidadoNemEntraEmDeadlock() throws Exception {
		var a=cadastrarNutri();String email=email();String p=criarFicha(a.token(),email);String codigo=texto(convidar(a.token(),p),"codigo");
		try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
			var resultados=executor.invokeAll(List.<Callable<Integer>>of(()->requisitar("POST","/pacientes/"+p+"/convite",a.token(),null).statusCode(),()->aceitar(codigo,null,email).statusCode()),30,TimeUnit.SECONDS);
			int gerar=resultados.get(0).get(),aceitar=resultados.get(1).get();
			assertThat((gerar==201&&aceitar==422)||(gerar==422&&aceitar==200)).isTrue();
		}
	}
	@Test void novoVinculoAposEncerramentoMantemFichaHistoricaIntocada() throws Exception {
		var a=cadastrarNutri();String email=email();String p=criarFicha(a.token(),email);
		var primeiro=json(aceitar(texto(convidar(a.token(),p),"codigo"),null,email));String token=primeiro.path("sessao").path("accessToken").asString();String antigo=primeiro.path("vinculo").path("id").asString();
		assertThat(requisitar("POST","/pacientes/"+p+"/encerrar-vinculo?vinculoId="+antigo,a.token(),null).statusCode()).isEqualTo(200);
		String novoPaciente=criarFicha(a.token(),email);var segundo=aceitar(texto(convidar(a.token(),novoPaciente),"codigo"),token,email);assertThat(segundo.statusCode()).as(segundo.body()).isEqualTo(200);
		assertThat(requisitar("PATCH","/pacientes/"+p,a.token(),Map.of("nome","Ficha atual fictícia"),Map.of("If-Match","\"1\"")).statusCode()).isEqualTo(200);
		assertThat(jdbc.queryForObject("select nome from ficha_paciente where vinculo_id=?",String.class,UUID.fromString(antigo))).isEqualTo("Paciente fictício");
		assertThat(jdbc.queryForObject("select count(*) from vinculo where nutricionista_id=? and paciente_id=?",Integer.class,a.nutri(),UUID.fromString(p))).isEqualTo(2);
	}
	@Test void corrigirEmailDaFichaRevogaConviteDoDestinatarioAnterior() throws Exception {
		var a=cadastrarNutri();String anterior=email(),novo=email();String p=criarFicha(a.token(),anterior);String codigo=texto(convidar(a.token(),p),"codigo");
		assertThat(requisitar("PATCH","/pacientes/"+p,a.token(),Map.of("nome","Paciente fictício","email",novo),Map.of("If-Match","\"0\"")).statusCode()).isEqualTo(200);
		erro(requisitar("GET","/convites/"+codigo,null,null),422,"CONVITE_INVALIDO");
		assertThat(aceitar(texto(convidar(a.token(),p),"codigo"),null,novo).statusCode()).isEqualTo(200);
	}
	String criarFicha(String token,String email) throws Exception {var corpo=new HashMap<String,Object>();corpo.put("nome","Paciente fictício");if(email!=null)corpo.put("email",email);var resposta=requisitar("POST","/pacientes",token,corpo);assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(201);return texto(json(resposta),"id");}
	JsonNode convidar(String token,String paciente) throws Exception {var resposta=requisitar("POST","/pacientes/"+paciente+"/convite",token,null);assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(201);return json(resposta);}
	HttpResponse<String> aceitar(String codigo,String token,String email) throws Exception{return requisitar("POST","/convites/"+codigo+"/aceitar",token,corpo(token,email));}
	Map<String,Object> corpo(String token,String email) {var corpo=new HashMap<String,Object>();corpo.put("aceitouTermo",true);corpo.put("versaoTermo","clinico-teste-v1");corpo.put("dispositivo",Map.of("id",UUID.randomUUID(),"plataforma","ANDROID","nomeAparelho","Aparelho fictício"));if(token==null)corpo.put("cadastro",Map.of("nome","Paciente fictício","email",email,"senha",SENHA));return corpo;}
	String email(){return UUID.randomUUID()+"@example.invalid";}
	String texto(JsonNode dados,String campo){return dados.path(campo).asString();}
}
