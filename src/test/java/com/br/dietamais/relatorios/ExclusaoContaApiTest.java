package com.br.dietamais.relatorios;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.util.*;
import org.junit.jupiter.api.Test;

class ExclusaoContaApiTest extends TesteComPaciente {
	@Test void pacienteExcluiAContaAnonimizaIdentidadeEMantemOProntuarioDaNutri() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var ali=alimentos(a.token(),1);
		var dieta=publicar(a.token(),criarDieta(a.token(),p.fichaId(),dieta("Plano retido",refeicao("Café",0,item(ali.get(0),"1")))));
		assertThat(requisitar("POST","/eu/exportacao",p.token(),null).statusCode()).isEqualTo(202);

		erro(requisitar("POST","/eu/excluir-conta",p.token(),Map.of("senha","senha errada")),401,"CREDENCIAIS_INVALIDAS");
		erro(requisitar("POST","/eu/excluir-conta",a.token(),Map.of("senha",SENHA)),403,"PERFIL_INVALIDO");
		erro(requisitar("POST","/eu/excluir-conta",p.token(),Map.of()),400,"OBRIGATORIO");
		assertThat(requisitar("POST","/eu/excluir-conta",null,Map.of("senha",SENHA)).statusCode()).isEqualTo(401);
		assertThat(requisitar("GET","/eu/dietas",p.token(),null).statusCode()).isEqualTo(200); // nada mudou com as tentativas falhas

		var r=requisitar("POST","/eu/excluir-conta",p.token(),Map.of("senha",SENHA));assertThat(r.statusCode()).as(r.body()).isEqualTo(204);

		// a conta morreu: token antigo, login e dados pessoais
		assertThat(requisitar("GET","/eu/dietas",p.token(),null).statusCode()).isEqualTo(401);
		var login=requisitar("POST","/auth/login",null,Map.of("email",p.email(),"senha",SENHA,"dispositivo",Map.of("id",UUID.randomUUID(),"plataforma","ANDROID","nomeAparelho","Aparelho fictício")));
		erro(login,401,"CREDENCIAIS_INVALIDAS");
		var u=jdbc.queryForMap("select nome,email,ativo,senha_hash from usuario where id=?::uuid",p.usuarioId());
		assertThat(u.get("nome")).isEqualTo("Titular removido");assertThat((String)u.get("email")).startsWith("removido-").doesNotContain(p.email());assertThat(u.get("ativo")).isEqualTo(false);
		assertThat((String)u.get("senha_hash")).doesNotStartWith("$2");
		var pac=jdbc.queryForMap("select nome,email,data_nascimento,sexo from paciente where usuario_id=?::uuid",p.usuarioId());
		assertThat(pac.get("nome")).isEqualTo("Titular removido");assertThat(pac.get("email")).isNull();
		assertThat(jdbc.queryForObject("select count(*) from refresh_token where usuario_id=?::uuid",Integer.class,p.usuarioId())).isZero();
		assertThat(jdbc.queryForObject("select count(*) from exportacao_dados where usuario_id=?::uuid",Integer.class,p.usuarioId())).isZero();
		assertThat(jdbc.queryForObject("select count(*) from dispositivo where usuario_id=?::uuid",Integer.class,p.usuarioId())).isZero();

		// a nutri continua com o prontuário (somente leitura) e vê o vínculo encerrado
		var ficha=json(requisitar("GET","/pacientes/"+p.fichaId(),a.token(),null));
		assertThat(ficha.path("status").asString()).isEqualTo("ENCERRADO");
		assertThat(requisitar("GET","/dietas/"+dieta.path("dietaId").asString(),a.token(),null).statusCode()).isEqualTo(200);
		// o consentimento é prova legal e permanece
		assertThat(jdbc.queryForObject("select count(*) from consentimento where usuario_id=?::uuid",Integer.class,p.usuarioId())).isPositive();
	}

	@Test void nutriExcluiFichaLogicamenteSoDeVinculoNaoAtivoEOProntuarioFicaNoBanco() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();
		var convidado=json(requisitar("POST","/pacientes",a.token(),Map.of("nome","Convidado fictício","email",UUID.randomUUID()+"@example.invalid")));
		String id=convidado.path("id").asString();
		erro(requisitar("DELETE","/pacientes/"+id,b.token(),null,Map.of("If-Match","\"1\"")),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("DELETE","/pacientes/"+id,a.token(),null,Map.of("If-Match","\"9\"")),409,"CONFLITO_VERSAO");
		assertThat(requisitar("DELETE","/pacientes/"+id,a.token(),null).statusCode()).isEqualTo(400);
		var etag=requisitar("GET","/pacientes/"+id,a.token(),null).headers().firstValue("ETag").orElseThrow();
		var ok=requisitar("DELETE","/pacientes/"+id,a.token(),null,Map.of("If-Match",etag));assertThat(ok.statusCode()).as(ok.body()).isEqualTo(204);
		erro(requisitar("GET","/pacientes/"+id,a.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		assertThat(json(requisitar("GET","/pacientes",a.token(),null)).path("itens")).isEmpty();
		assertThat(jdbc.queryForObject("select excluido_em is not null from ficha_paciente where paciente_id=?::uuid",Boolean.class,id)).isTrue(); // retido
		assertThat(jdbc.queryForObject("select status from vinculo where paciente_id=?::uuid",String.class,id)).isEqualTo("ENCERRADO");

		var p=pacienteVinculado(a);
		var etagAtivo=requisitar("GET","/pacientes/"+p.fichaId(),a.token(),null).headers().firstValue("ETag").orElseThrow();
		erro(requisitar("DELETE","/pacientes/"+p.fichaId(),a.token(),null,Map.of("If-Match",etagAtivo)),422,"VINCULO_ATIVO");
		assertThat(requisitar("POST","/pacientes/"+p.fichaId()+"/encerrar-vinculo",a.token(),null).statusCode()).isEqualTo(200);
		var versao=requisitar("GET","/pacientes/"+p.fichaId(),a.token(),null).headers().firstValue("ETag").orElseThrow();
		assertThat(requisitar("DELETE","/pacientes/"+p.fichaId(),a.token(),null,Map.of("If-Match",versao)).statusCode()).isEqualTo(204);
		erro(requisitar("GET","/pacientes/"+p.fichaId(),a.token(),null),404,"RECURSO_NAO_ENCONTRADO");
	}
}
