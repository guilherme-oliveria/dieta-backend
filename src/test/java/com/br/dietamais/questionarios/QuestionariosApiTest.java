package com.br.dietamais.questionarios;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class QuestionariosApiTest extends TesteComPaciente {
	JsonNode modeloPadrao(String token) throws Exception {
		var r=requisitar("GET","/modelos-questionario",token,null);assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
		var itens=json(r).path("itens");assertThat(itens).hasSize(1);return itens.get(0);
	}
	JsonNode pergunta(JsonNode blocosOuQuestionario,String texto){
		for(var b:blocosOuQuestionario.path("blocos"))for(var p:b.path("perguntas"))if(p.path("texto").asString().equals(texto))return p;
		throw new AssertionError("pergunta não encontrada: "+texto);
	}
	Map<String,Object> modeloMinimo(String nome){
		return Map.of("nome",nome,"blocos",List.of(Map.of("titulo","Bloco","perguntas",List.of(Map.of("texto","Pergunta","tipo","TEXTO","obrigatoria",true)))));
	}
	JsonNode enviar(String token,String ficha,String modelo) throws Exception {
		var r=requisitar("POST","/pacientes/"+ficha+"/questionarios",token,Map.of("modeloId",modelo));assertThat(r.statusCode()).as(r.body()).isEqualTo(201);return json(r);
	}

	@Test void modeloPadraoNasceUmaVezEFoiEditavelComIfMatchEIsolamento() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();
		var padrao=modeloPadrao(a.token());
		assertThat(padrao.path("padrao").asBoolean()).isTrue();assertThat(padrao.path("blocos")).hasSize(12);
		assertThat(modeloPadrao(a.token()).path("id").asString()).isEqualTo(padrao.path("id").asString()); // segunda leitura não duplica
		var sexo=pergunta(padrao,"Sexo");var gravidez=pergunta(padrao,"Seu ciclo menstrual é regular?");
		assertThat(gravidez.path("condicao").path("perguntaId").asString()).isEqualTo(sexo.path("id").asString());
		assertThat(modeloPadrao(b.token()).path("id").asString()).isNotEqualTo(padrao.path("id").asString());

		String id=padrao.path("id").asString();var corpo=new HashMap<String,Object>();corpo.put("nome","Meu modelo");corpo.put("blocos",mapper.convertValue(padrao.path("blocos"),Object.class));
		erro(requisitar("PUT","/modelos-questionario/"+id,a.token(),corpo),400,"FORMATO_INVALIDO"); // sem If-Match
		var ok=requisitar("PUT","/modelos-questionario/"+id,a.token(),corpo,Map.of("If-Match","\"1\""));assertThat(ok.statusCode()).as(ok.body()).isEqualTo(200);
		assertThat(json(ok).path("versao").asLong()).isEqualTo(2);assertThat(json(ok).path("nome").asString()).isEqualTo("Meu modelo");
		var conflito=requisitar("PUT","/modelos-questionario/"+id,a.token(),corpo,Map.of("If-Match","\"1\""));erro(conflito,409,"CONFLITO_VERSAO");assertThat(json(conflito).path("atual").path("versao").asLong()).isEqualTo(2);
		erro(requisitar("GET","/modelos-questionario/"+id,b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("PUT","/modelos-questionario/"+id,b.token(),corpo,Map.of("If-Match","\"2\"")),404,"RECURSO_NAO_ENCONTRADO");
		var novo=requisitar("POST","/modelos-questionario",a.token(),modeloMinimo("Curto"));assertThat(novo.statusCode()).as(novo.body()).isEqualTo(201);
		assertThat(json(requisitar("GET","/modelos-questionario",a.token(),null)).path("itens")).hasSize(2);
		assertThat(requisitar("GET","/modelos-questionario",null,null).statusCode()).isEqualTo(401);
	}

	@Test void modeloInvalidoRecebe400ComCampo() throws Exception {
		var a=cadastrarNutri();
		var semOpcoes=Map.of("nome","x","blocos",List.of(Map.of("titulo","B","perguntas",List.of(Map.of("texto","P","tipo","ESCOLHA_UNICA")))));
		var r=requisitar("POST","/modelos-questionario",a.token(),semOpcoes);erro(r,400,"FORMATO_INVALIDO");assertThat(r.body()).contains("opcoes");assertThat(json(r).path("campos").get(0).path("campo").asString()).isEqualTo("blocos[0].perguntas[0].opcoes");
		var condicaoFutura=Map.of("nome","x","blocos",List.of(Map.of("titulo","B","perguntas",List.of(
			Map.of("texto","P","tipo","TEXTO","condicao",Map.of("perguntaId",UUID.randomUUID().toString(),"operador","IGUAL","valor","a"))))));
		erro(requisitar("POST","/modelos-questionario",a.token(),condicaoFutura),400,"FORMATO_INVALIDO");
		var opcaoSobrando=Map.of("nome","x","blocos",List.of(Map.of("titulo","B","perguntas",List.of(Map.of("texto","P","tipo","TEXTO","opcoes",List.of("a","b"))))));
		erro(requisitar("POST","/modelos-questionario",a.token(),opcaoSobrando),400,"FORMATO_INVALIDO");
	}

	@Test void envioCongelaPerguntasPacienteSalvaParcialEConcluiImutavel() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var outro=pacienteVinculado(a);var b=cadastrarNutri();
		var padrao=modeloPadrao(a.token());String modelo=padrao.path("id").asString();
		var q=enviar(a.token(),p.fichaId(),modelo);String id=q.path("id").asString();
		assertThat(q.path("status").asString()).isEqualTo("PENDENTE");assertThat(q.path("blocos")).hasSize(12);assertThat(q.path("respostas").size()).isZero();
		// RN-NOT-01: o envio vira aviso para o paciente, com texto genérico e rota do questionário
		assertThat(jdbc.queryForObject("select rota from aviso where usuario_id=?::uuid and tipo='QUESTIONARIO_ENVIADO'",String.class,p.usuarioId())).isEqualTo("/questionarios/"+id);

		// RN-ANA-04: mudar o modelo depois não muda o questionário enviado
		var edicao=new HashMap<String,Object>();edicao.put("nome","Outro nome");edicao.put("blocos",List.of(Map.of("titulo","Só um","perguntas",List.of(Map.of("texto","Nova","tipo","TEXTO")))));
		var edicaoResp=requisitar("PUT","/modelos-questionario/"+modelo,a.token(),edicao,Map.of("If-Match","\"1\""));assertThat(edicaoResp.statusCode()).as(edicaoResp.body()).isEqualTo(200);
		var meus=json(requisitar("GET","/eu/questionarios",p.token(),null)).path("itens");assertThat(meus).hasSize(1);assertThat(meus.get(0).path("blocos")).hasSize(12);

		// salvar parcial: tipos e perguntas validados
		String objetivo=pergunta(q,"Qual é o seu principal objetivo com a dieta?").path("id").asString(),sexo=pergunta(q,"Sexo").path("id").asString();
		String esc=pergunta(q,"Frutas e verduras").path("id").asString();
		var parcial=requisitar("PUT","/eu/questionarios/"+id+"/respostas",p.token(),Map.of("respostas",Map.of(objetivo,"Emagrecer")));
		assertThat(parcial.statusCode()).as(parcial.body()).isEqualTo(200);assertThat(json(parcial).path("respostas").path(objetivo).asString()).isEqualTo("Emagrecer");assertThat(json(parcial).path("versao").asLong()).isEqualTo(2);
		erro(requisitar("PUT","/eu/questionarios/"+id+"/respostas",p.token(),Map.of("respostas",Map.of(objetivo,"Invalido"))),400,"FORMATO_INVALIDO");
		erro(requisitar("PUT","/eu/questionarios/"+id+"/respostas",p.token(),Map.of("respostas",Map.of(esc,9))),400,"FORMATO_INVALIDO");
		erro(requisitar("PUT","/eu/questionarios/"+id+"/respostas",p.token(),Map.of("respostas",Map.of(UUID.randomUUID().toString(),"x"))),400,"FORMATO_INVALIDO");

		// isolamento: outro paciente, outra nutri e a nutri tentando responder
		erro(requisitar("PUT","/eu/questionarios/"+id+"/respostas",outro.token(),Map.of("respostas",Map.of(objetivo,"Emagrecer"))),404,"RECURSO_NAO_ENCONTRADO");
		assertThat(json(requisitar("GET","/eu/questionarios",outro.token(),null)).path("itens")).isEmpty();
		erro(requisitar("GET","/pacientes/"+p.fichaId()+"/questionarios",b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("PUT","/eu/questionarios/"+id+"/respostas",a.token(),Map.of("respostas",Map.of(objetivo,"Emagrecer"))),403,"PERFIL_INVALIDO");

		// concluir sem as obrigatórias visíveis: lista o que falta; "Saúde da mulher" não conta enquanto o sexo não for feminino
		var falta=requisitar("POST","/eu/questionarios/"+id+"/enviar",p.token(),null);erro(falta,400,"OBRIGATORIO");
		var campos=new HashSet<String>();json(falta).path("campos").forEach(c->campos.add(c.path("campo").asString()));
		assertThat(campos).contains(sexo,pergunta(q,"Você pratica atividade física?").path("id").asString()).doesNotContain(objetivo);
		String treino=pergunta(q,"Você pratica atividade física?").path("id").asString();
		assertThat(requisitar("PUT","/eu/questionarios/"+id+"/respostas",p.token(),Map.of("respostas",Map.of(sexo,"Feminino",treino,"Não"))).statusCode()).isEqualTo(200);
		// agora é feminino: as condicionais passam a existir, mas nenhuma delas é obrigatória
		var pronto=requisitar("POST","/eu/questionarios/"+id+"/enviar",p.token(),null);assertThat(pronto.statusCode()).as(pronto.body()).isEqualTo(200);
		assertThat(json(pronto).path("status").asString()).isEqualTo("RESPONDIDO");assertThat(json(pronto).path("respondidoEm").isNull()).isFalse();
		assertThat(jdbc.queryForObject("select count(*) from aviso where usuario_id=? and tipo='QUESTIONARIO_RESPONDIDO'",Integer.class,a.usuario())).isOne();

		// imutável (RN-ANA-03) e a nutri lê as respostas
		erro(requisitar("PUT","/eu/questionarios/"+id+"/respostas",p.token(),Map.of("respostas",Map.of(objetivo,"Melhorar a saúde"))),409,"QUESTIONARIO_FECHADO");
		erro(requisitar("POST","/eu/questionarios/"+id+"/enviar",p.token(),null),409,"QUESTIONARIO_FECHADO");
		var lido=json(requisitar("GET","/pacientes/"+p.fichaId()+"/questionarios",a.token(),null)).path("itens").get(0);
		assertThat(lido.path("status").asString()).isEqualTo("RESPONDIDO");assertThat(lido.path("respostas").path(sexo).asString()).isEqualTo("Feminino");
		assertThat(jdbc.queryForObject("select count(*) from resposta where questionario_id=?",Integer.class,UUID.fromString(id))).isEqualTo(3);
		// o banco também recusa mexer (defesa em profundidade)
		assertThatThrownBy(()->jdbc.update("update resposta set valor='\"x\"'::jsonb where questionario_id=?",UUID.fromString(id))).hasMessageContaining("imutáveis");
	}

	@Test void envioExigeVinculoAtivoAssinaturaEIdempotencia() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);String modelo=modeloPadrao(a.token()).path("id").asString();
		erro(requisitar("POST","/pacientes/"+p.fichaId()+"/questionarios",p.token(),Map.of("modeloId",modelo)),403,"PERFIL_INVALIDO");
		erro(requisitar("POST","/pacientes/"+p.fichaId()+"/questionarios",a.token(),Map.of("modeloId",UUID.randomUUID().toString())),404,"RECURSO_NAO_ENCONTRADO");
		var chave=UUID.randomUUID().toString();
		var r1=requisitar("POST","/pacientes/"+p.fichaId()+"/questionarios",a.token(),Map.of("modeloId",modelo),Map.of("Idempotency-Key",chave));
		var r2=requisitar("POST","/pacientes/"+p.fichaId()+"/questionarios",a.token(),Map.of("modeloId",modelo),Map.of("Idempotency-Key",chave));
		assertThat(r1.statusCode()).isEqualTo(201);assertThat(r2.statusCode()).isEqualTo(201);assertThat(json(r2).path("id").asString()).isEqualTo(json(r1).path("id").asString());
		assertThat(jdbc.queryForObject("select count(*) from questionario where vinculo_id=?",Integer.class,UUID.fromString(p.vinculoId()))).isEqualTo(1);
		assertThat(requisitar("POST","/pacientes/"+p.fichaId()+"/encerrar-vinculo",a.token(),null).statusCode()).isEqualTo(200);
		erro(requisitar("POST","/pacientes/"+p.fichaId()+"/questionarios",a.token(),Map.of("modeloId",modelo)),403,"ACESSO_NEGADO");
		// vínculo encerrado: o paciente continua lendo, mas não responde mais
		var meus=json(requisitar("GET","/eu/questionarios",p.token(),null)).path("itens");assertThat(meus).hasSize(1);
		String qid=meus.get(0).path("id").asString(),campo=pergunta(meus.get(0),"Sexo").path("id").asString();
		erro(requisitar("PUT","/eu/questionarios/"+qid+"/respostas",p.token(),Map.of("respostas",Map.of(campo,"Feminino"))),403,"ACESSO_NEGADO");
	}

	@Test void syncEntregaModelosEQuestionariosEAceitaRespostasParciaisOffline() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var padrao=modeloPadrao(a.token());
		var q=enviar(a.token(),p.fichaId(),padrao.path("id").asString());String id=q.path("id").asString();String objetivo=pergunta(q,"Qual é o seu principal objetivo com a dieta?").path("id").asString();
		var nutri=json(requisitar("GET","/sync?limite=1000",a.token(),null)).path("mudancas");
		assertThat(nutri.path("modelos")).hasSize(1);assertThat(nutri.path("questionarios")).hasSize(1);
		var paciente=json(requisitar("GET","/sync?limite=1000",p.token(),null));
		assertThat(paciente.path("mudancas").path("questionarios")).hasSize(1);assertThat(paciente.path("mudancas").path("modelos").size()).isZero();
		String cursor=paciente.path("cursor").asString();

		String m1=UUID.randomUUID().toString();
		var lote=Map.of("mutacoes",List.of(Map.of("idMutacao",m1,"entidade","questionarios","operacao","ATUALIZAR","id",id,"versaoBase",1,"dados",Map.of("respostas",Map.of(objetivo,"Emagrecer")))));
		var resp=requisitar("POST","/sync",p.token(),lote);assertThat(resp.statusCode()).as(resp.body()).isEqualTo(200);
		assertThat(json(resp).path("resultados").get(0).path("status").asString()).isEqualTo("APLICADA");assertThat(json(resp).path("resultados").get(0).path("versao").asLong()).isEqualTo(2);
		assertThat(json(requisitar("POST","/sync",p.token(),lote)).path("resultados").get(0).path("versao").asLong()).isEqualTo(2); // reenvio devolve o mesmo resultado
		var depois=json(requisitar("GET","/sync?limite=1000&cursor="+cursor,p.token(),null)).path("mudancas").path("questionarios");
		assertThat(depois).hasSize(1);assertThat(depois.get(0).path("dados").path("respostas").path(objetivo).asString()).isEqualTo("Emagrecer");
		var invalida=Map.of("mutacoes",List.of(Map.of("idMutacao",UUID.randomUUID().toString(),"entidade","questionarios","operacao","ATUALIZAR","id",id,"dados",Map.of("respostas",Map.of(objetivo,"Nada")))));
		assertThat(json(requisitar("POST","/sync",p.token(),invalida)).path("resultados").get(0).path("codigo").asString()).isEqualTo("FORMATO_INVALIDO");
		var naoPaciente=Map.of("mutacoes",List.of(Map.of("idMutacao",UUID.randomUUID().toString(),"entidade","questionarios","operacao","ATUALIZAR","id",id,"dados",Map.of("respostas",Map.of(objetivo,"Emagrecer")))));
		assertThat(json(requisitar("POST","/sync",a.token(),naoPaciente)).path("resultados").get(0).path("status").asString()).isEqualTo("REJEITADA");
	}
}
