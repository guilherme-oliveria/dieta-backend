package com.br.dietamais.rotina;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class RotinaApiTest extends TesteComPaciente {
	static final ZoneId FUSO=ZoneId.of("America/Sao_Paulo");
	record Cenario(ContaTeste nutri,PacienteTeste paciente,JsonNode snapshot,String refeicao,String item,String grupoItem,String trocaValida,String trocaOutroGrupo) {}
	Cenario cenario() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);var ali=alimentos(a.token(),1);
		var s=publicar(a.token(),criarDieta(a.token(),p.fichaId(),dieta("Rotina",refeicao("Café",0,item(ali.get(0),"1")))));
		var it=s.path("refeicoes").get(0).path("itens").get(0);String grupo=it.path("grupoId").asString();String troca=null,outro=null;
		for(var x:s.path("listaSubstituicao").path("alimentos")){
			if(x.path("grupoId").asString().equals(grupo)&&!x.path("id").asString().equals(it.path("alimentoId").asString())&&troca==null)troca=x.path("id").asString();
			if(!x.path("grupoId").asString().equals(grupo)&&outro==null)outro=x.path("id").asString();
		}
		return new Cenario(a,p,s,s.path("refeicoes").get(0).path("id").asString(),it.path("id").asString(),grupo,troca,outro);
	}
	Map<String,Object> checkin(Cenario c,String status,LocalDate data){
		var m=new HashMap<String,Object>();m.put("dietaId",c.snapshot().path("dietaId").asString());m.put("refeicaoId",c.refeicao());m.put("data",data.toString());m.put("status",status);m.put("registradoEm",Instant.now().toString());return m;
	}
	LocalDate hoje(){return LocalDate.now(FUSO);}
	String novoId(){return jdbc.queryForObject("select uuidv7()",String.class);}
	@Test void checkinSoInclusaoComRegrasDeJanelaTrocaEIsolamento() throws Exception {
		var c=cenario();var t=c.paciente().token();
		var corpo=checkin(c,"FEITO",hoje());corpo.put("id",novoId());
		var r1=requisitar("POST","/eu/checkins",t,corpo);assertThat(r1.statusCode()).as(r1.body()).isEqualTo(201);
		var r2=requisitar("POST","/eu/checkins",t,corpo);assertThat(r2.statusCode()).isEqualTo(201);assertThat(json(r2)).isEqualTo(json(r1));
		assertThat(jdbc.queryForObject("select count(*) from checkin where paciente_id=?",Integer.class,UUID.fromString(json(r1).path("pacienteId").asString()))).isEqualTo(1);
		var troca=checkin(c,"TROCADO",hoje().minusDays(1));troca.put("trocas",List.of(Map.of("itemId",c.item(),"alimentoId",c.trocaValida())));
		var rt=requisitar("POST","/eu/checkins",t,troca);assertThat(rt.statusCode()).as(rt.body()).isEqualTo(201);assertThat(json(rt).path("trocas")).hasSize(1);
		var grupoErrado=checkin(c,"TROCADO",hoje());grupoErrado.put("trocas",List.of(Map.of("itemId",c.item(),"alimentoId",c.trocaOutroGrupo())));erro(requisitar("POST","/eu/checkins",t,grupoErrado),400,"FORMATO_INVALIDO");
		var semTroca=checkin(c,"TROCADO",hoje());erro(requisitar("POST","/eu/checkins",t,semTroca),400,"FORMATO_INVALIDO");
		var puladoComTroca=checkin(c,"PULADO",hoje());puladoComTroca.put("trocas",List.of(Map.of("itemId",c.item(),"alimentoId",c.trocaValida())));erro(requisitar("POST","/eu/checkins",t,puladoComTroca),400,"FORMATO_INVALIDO");
		erro(requisitar("POST","/eu/checkins",t,checkin(c,"FEITO",hoje().minusDays(4))),400,"FORMATO_INVALIDO");
		assertThat(requisitar("POST","/eu/checkins",t,checkin(c,"FEITO",hoje().minusDays(3))).statusCode()).isEqualTo(201);
		erro(requisitar("POST","/eu/checkins",t,checkin(c,"FEITO",hoje().plusDays(1))),400,"FORMATO_INVALIDO");
		var refeicaoInexistente=checkin(c,"FEITO",hoje());refeicaoInexistente.put("refeicaoId",UUID.randomUUID().toString());erro(requisitar("POST","/eu/checkins",t,refeicaoInexistente),404,"RECURSO_NAO_ENCONTRADO");
		// "o último vale": dois registros para a mesma refeição/dia coexistem (inclusão), sem conflito
		assertThat(requisitar("POST","/eu/checkins",t,checkin(c,"PULADO",hoje())).statusCode()).isEqualTo(201);
		// outro paciente não usa a dieta alheia e não toma o id
		var outro=pacienteVinculado(c.nutri());erro(requisitar("POST","/eu/checkins",outro.token(),checkin(c,"FEITO",hoje())),404,"RECURSO_NAO_ENCONTRADO");
		var roubo=checkin(c,"FEITO",hoje());erro(requisitar("POST","/eu/checkins",c.nutri().token(),roubo),403,"PERFIL_INVALIDO");
		var colisao=checkin(c,"FEITO",hoje());colisao.put("id",corpo.get("id"));
		var outroSnapshot=cenario();var colisao2=checkin(outroSnapshot,"FEITO",hoje());colisao2.put("id",corpo.get("id"));erro(requisitar("POST","/eu/checkins",outroSnapshot.paciente().token(),colisao2),409,"CONFLITO_VERSAO");
		assertThat(requisitar("POST","/eu/checkins",null,corpo).statusCode()).isEqualTo(401);
		assertThat(requisitar("POST","/pacientes/"+c.paciente().fichaId()+"/encerrar-vinculo",c.nutri().token(),null).statusCode()).isEqualTo(200);
		erro(requisitar("POST","/eu/checkins",t,checkin(c,"FEITO",hoje())),403,"ACESSO_NEGADO");
	}
	@Test void aguaEPesoValidamIntervalosEJanela() throws Exception {
		var c=cenario();var t=c.paciente().token();
		assertThat(requisitar("POST","/eu/agua",t,Map.of("id",novoId(),"ml",250,"registradoEm",Instant.now().toString())).statusCode()).isEqualTo(201);
		assertThat(requisitar("POST","/eu/agua",t,Map.of("ml",5000,"registradoEm",Instant.now().toString())).statusCode()).isEqualTo(201);
		erro(requisitar("POST","/eu/agua",t,Map.of("ml",5001,"registradoEm",Instant.now().toString())),400,"FORMATO_INVALIDO");
		erro(requisitar("POST","/eu/agua",t,Map.of("ml",0,"registradoEm",Instant.now().toString())),400,"FORMATO_INVALIDO");
		erro(requisitar("POST","/eu/agua",t,Map.of("ml",200,"registradoEm",Instant.now().plusSeconds(3600).toString())),400,"FORMATO_INVALIDO");
		var peso=Map.<String,Object>of("id",novoId(),"pesoKg",72.5,"registradoEm",Instant.now().toString());
		var p1=requisitar("POST","/eu/pesos",t,peso);assertThat(p1.statusCode()).as(p1.body()).isEqualTo(201);assertThat(json(requisitar("POST","/eu/pesos",t,peso))).isEqualTo(json(p1));
		erro(requisitar("POST","/eu/pesos",t,Map.of("pesoKg",19.9,"registradoEm",Instant.now().toString())),422,"VALOR_FORA_DO_INTERVALO");
		erro(requisitar("POST","/eu/pesos",t,Map.of("pesoKg",400.1,"registradoEm",Instant.now().toString())),422,"VALOR_FORA_DO_INTERVALO");
		erro(requisitar("POST","/eu/pesos",c.nutri().token(),Map.of("pesoKg",70,"registradoEm",Instant.now().toString())),403,"PERFIL_INVALIDO");
	}
	@Test void postSyncAplicaCriarIdempotenteComResultadoPorMutacaoEIsolamento() throws Exception {
		var c=cenario();var t=c.paciente().token();
		String m1=UUID.randomUUID().toString(),m2=UUID.randomUUID().toString(),m3=UUID.randomUUID().toString(),m4=UUID.randomUUID().toString(),m5=UUID.randomUUID().toString();
		String idCheckin=novoId();var dadosCheckin=checkin(c,"FEITO",hoje());
		var lote=Map.of("mutacoes",List.of(
			Map.of("idMutacao",m1,"entidade","checkins","operacao","CRIAR","id",idCheckin,"dados",dadosCheckin),
			Map.of("idMutacao",m2,"entidade","agua","operacao","CRIAR","id",novoId(),"dados",Map.of("ml",300,"registradoEm",Instant.now().toString())),
			Map.of("idMutacao",m3,"entidade","pesos","operacao","CRIAR","id",novoId(),"dados",Map.of("pesoKg",10,"registradoEm",Instant.now().toString())),
			Map.of("idMutacao",m4,"entidade","dietas","operacao","ATUALIZAR","id",novoId(),"versaoBase",1,"dados",Map.of("titulo","x")),
			Map.of("idMutacao",m5,"entidade","checkins","operacao","ATUALIZAR","id",idCheckin,"versaoBase",1,"dados",dadosCheckin)));
		var r=requisitar("POST","/sync",t,lote);assertThat(r.statusCode()).as(r.body()).isEqualTo(200);var res=json(r).path("resultados");assertThat(res).hasSize(5);
		assertThat(res.get(0).path("status").asString()).isEqualTo("APLICADA");assertThat(res.get(0).path("idMutacao").asString()).isEqualTo(m1);
		assertThat(res.get(1).path("status").asString()).isEqualTo("APLICADA");
		assertThat(res.get(2).path("status").asString()).isEqualTo("REJEITADA");assertThat(res.get(2).path("codigo").asString()).isEqualTo("VALOR_FORA_DO_INTERVALO");
		assertThat(res.get(3).path("status").asString()).isEqualTo("REJEITADA");assertThat(res.get(4).path("status").asString()).isEqualTo("REJEITADA");
		// reenvio idêntico devolve o mesmo resultado sem duplicar
		var repetido=requisitar("POST","/sync",t,lote);assertThat(json(repetido).path("resultados")).isEqualTo(res);
		assertThat(jdbc.queryForObject("select count(*) from checkin where id=?",Integer.class,UUID.fromString(idCheckin))).isEqualTo(1);
		// idMutacao de outro usuário não reproduz resultado alheio
		var outro=cenario();var invasao=requisitar("POST","/sync",outro.paciente().token(),Map.of("mutacoes",List.of(Map.of("idMutacao",m2,"entidade","agua","operacao","CRIAR","id",novoId(),"dados",Map.of("ml",100,"registradoEm",Instant.now().toString())))));
		assertThat(json(invasao).path("resultados").get(0).path("status").asString()).isEqualTo("REJEITADA");assertThat(json(invasao).path("resultados").get(0).path("codigo").asString()).isEqualTo("CHAVE_IDEMPOTENCIA_REUTILIZADA");
		// nutri não escreve rotina do paciente
		var nutri=requisitar("POST","/sync",c.nutri().token(),Map.of("mutacoes",List.of(Map.of("idMutacao",UUID.randomUUID().toString(),"entidade","agua","operacao","CRIAR","id",novoId(),"dados",Map.of("ml",100,"registradoEm",Instant.now().toString())))));
		assertThat(json(nutri).path("resultados").get(0).path("codigo").asString()).isEqualTo("PERFIL_INVALIDO");
		// limites do lote
		erro(requisitar("POST","/sync",t,Map.of("mutacoes",List.of())),400,"FORMATO_INVALIDO");
		var grande=new ArrayList<Object>();for(int i=0;i<201;i++)grande.add(Map.of("idMutacao",UUID.randomUUID().toString(),"entidade","agua","operacao","CRIAR","id",novoId(),"dados",Map.of("ml",1,"registradoEm",Instant.now().toString())));
		erro(requisitar("POST","/sync",t,Map.of("mutacoes",grande)),400,"FORMATO_INVALIDO");
		assertThat(requisitar("POST","/sync",null,lote).statusCode()).isEqualTo(401);
	}
	@Test void syncDeLeituraEntregaRotinaAoPacienteEAoNutriDoVinculoSemVazar() throws Exception {
		var c=cenario();var b=cadastrarNutri();var t=c.paciente().token();
		assertThat(requisitar("POST","/eu/checkins",t,checkin(c,"FEITO",hoje())).statusCode()).isEqualTo(201);
		assertThat(requisitar("POST","/eu/agua",t,Map.of("ml",250,"registradoEm",Instant.now().toString())).statusCode()).isEqualTo(201);
		assertThat(requisitar("POST","/eu/pesos",t,Map.of("pesoKg",70.5,"registradoEm",Instant.now().toString())).statusCode()).isEqualTo(201);
		var paciente=sync(t);assertThat(paciente.path("checkins")).hasSize(1);assertThat(paciente.path("agua")).hasSize(1);assertThat(paciente.path("pesos")).hasSize(1);
		var nutri=sync(c.nutri().token());assertThat(nutri.path("checkins")).hasSize(1);assertThat(nutri.path("pesos")).hasSize(1);assertThat(nutri.path("agua")).isEmpty();
		assertThat(nutri.path("checkins").get(0).path("dados").path("pacienteId").asString()).isNotBlank();
		var alheia=sync(b.token());assertThat(alheia.path("checkins")).isEmpty();assertThat(alheia.path("pesos")).isEmpty();
		assertThat(jdbc.queryForObject("select count(*) from log_acesso where usuario_id=? and entidade='checkin'",Integer.class,c.nutri().usuario())).isGreaterThanOrEqualTo(1);
	}
	JsonNode sync(String token) throws Exception {
		var r=requisitar("GET","/sync?limite=1000",token,null);assertThat(r.statusCode()).as(r.body()).isEqualTo(200);var todas=mapper.createObjectNode();
		JsonNode corpo=json(r);
		while(true){
			corpo.path("mudancas").properties().forEach(e->{var arr=(tools.jackson.databind.node.ArrayNode)todas.withArray(e.getKey());e.getValue().forEach(arr::add);});
			if(!corpo.path("temMais").asBoolean())return todas;
			corpo=json(requisitar("GET","/sync?limite=1000&cursor="+corpo.path("cursor").asString(),token,null));
		}
	}
	@Test void consultarRotinaDevolveSoOsRegistrosDoProprioPacienteNoPeriodo() throws Exception {
		var c=cenario();var t=c.paciente().token();
		var feito=checkin(c,"FEITO",hoje());assertThat(requisitar("POST","/eu/checkins",t,feito).statusCode()).isEqualTo(201);
		assertThat(requisitar("POST","/eu/agua",t,Map.of("ml",250,"registradoEm",Instant.now().toString())).statusCode()).isEqualTo(201);
		assertThat(requisitar("POST","/eu/pesos",t,Map.of("pesoKg",70.5,"registradoEm",Instant.now().toString())).statusCode()).isEqualTo(201);
		var hojeTexto=hoje().toString();
		var r=json(requisitar("GET","/eu/rotina?de="+hojeTexto+"&ate="+hojeTexto,t,null));
		assertThat(r.path("checkins")).hasSize(1);assertThat(r.path("agua")).hasSize(1);assertThat(r.path("pesos")).hasSize(1);
		assertThat(r.path("agua").get(0).path("ml").asInt()).isEqualTo(250);
		// período sem registros
		var vazio=json(requisitar("GET","/eu/rotina?de="+hoje().minusDays(20)+"&ate="+hoje().minusDays(10),t,null));
		assertThat(vazio.path("checkins")).isEmpty();assertThat(vazio.path("agua")).isEmpty();assertThat(vazio.path("pesos")).isEmpty();
		// isolamento: outro paciente da mesma nutri não vê nada; a nutri não usa a rota do paciente
		var outro=pacienteVinculado(c.nutri());
		var deOutro=json(requisitar("GET","/eu/rotina?de="+hojeTexto+"&ate="+hojeTexto,outro.token(),null));
		assertThat(deOutro.path("checkins")).isEmpty();assertThat(deOutro.path("agua")).isEmpty();assertThat(deOutro.path("pesos")).isEmpty();
		erro(requisitar("GET","/eu/rotina?de="+hojeTexto+"&ate="+hojeTexto,c.nutri().token(),null),403,"PERFIL_INVALIDO");
		assertThat(requisitar("GET","/eu/rotina?de="+hojeTexto+"&ate="+hojeTexto,null,null).statusCode()).isEqualTo(401);
		// janela inválida: ao contrário ou acima de 366 dias
		erro(requisitar("GET","/eu/rotina?de="+hoje()+"&ate="+hoje().minusDays(1),t,null),400,"FORMATO_INVALIDO");
		erro(requisitar("GET","/eu/rotina?de="+hoje().minusDays(366)+"&ate="+hoje(),t,null),400,"FORMATO_INVALIDO");
		assertThat(requisitar("GET","/eu/rotina?de="+hoje().minusDays(365)+"&ate="+hoje(),t,null).statusCode()).isEqualTo(200);
	}
}
