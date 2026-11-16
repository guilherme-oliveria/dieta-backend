package com.br.dietamais.agenda;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.Test;

/** BE-65 (G-20), RN-FIN-01: comparecimento, valor e forma de pagamento da consulta; só da nutri, nunca do paciente. */
class FinanceiroApiTest extends TesteComPaciente {
	String agendar(String token,String ficha,int dias) throws Exception {
		var m=new HashMap<String,Object>();m.put("pacienteId",ficha);m.put("inicio",Instant.now().plus(dias,ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString());m.put("duracaoMin",40);m.put("tipo","RETORNO");
		var r=requisitar("POST","/consultas",token,m);assertThat(r.statusCode()).as(r.body()).isEqualTo(201);return json(r).path("id").asString();
	}
	Map<String,Object> corpo(String comparecimento,Object valor,String forma,boolean pago){
		var m=new HashMap<String,Object>();m.put("comparecimento",comparecimento);m.put("valor",valor);m.put("formaPagamento",forma);m.put("pago",pago);return m;
	}

	@Test void lancaFinanceiroComVersaoEIsolaDaOutraNutriEDoPaciente() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var p=pacienteVinculado(a);String id=agendar(a.token(),p.fichaId(),1);
		var vazio=requisitar("GET","/consultas/"+id+"/financeiro",a.token(),null);assertThat(vazio.statusCode()).isEqualTo(200);
		assertThat(json(vazio).path("comparecimento").asString()).isEqualTo("PENDENTE");assertThat(vazio.headers().firstValue("ETag").orElseThrow()).isEqualTo("\"0\"");
		// primeiro lançamento com If-Match "0"; versão errada dá 409
		erro(requisitar("PUT","/consultas/"+id+"/financeiro",a.token(),corpo("COMPARECEU",new BigDecimal("250.00"),"PIX",true),Map.of("If-Match","\"3\"")),409,"CONFLITO_VERSAO");
		var feito=requisitar("PUT","/consultas/"+id+"/financeiro",a.token(),corpo("COMPARECEU",new BigDecimal("250.00"),"PIX",true),Map.of("If-Match","\"0\""));
		assertThat(feito.statusCode()).as(feito.body()).isEqualTo(200);var f=json(feito);
		assertThat(f.path("valor").decimalValue()).isEqualByComparingTo("250");assertThat(f.path("formaPagamento").asString()).isEqualTo("PIX");assertThat(f.path("pago").asBoolean()).isTrue();
		assertThat(feito.headers().firstValue("ETag").orElseThrow()).isEqualTo("\"1\"");
		var segundo=requisitar("PUT","/consultas/"+id+"/financeiro",a.token(),corpo("COMPARECEU",new BigDecimal("300"),"DINHEIRO",true),Map.of("If-Match","\"1\""));
		assertThat(segundo.statusCode()).isEqualTo(200);assertThat(segundo.headers().firstValue("ETag").orElseThrow()).isEqualTo("\"2\"");
		// regras: pago exige valor e forma e comparecimento; valor negativo é inválido
		erro(requisitar("PUT","/consultas/"+id+"/financeiro",a.token(),corpo("COMPARECEU",null,"PIX",true),Map.of("If-Match","\"2\"")),400,"FORMATO_INVALIDO");
		erro(requisitar("PUT","/consultas/"+id+"/financeiro",a.token(),corpo("COMPARECEU",new BigDecimal("10"),null,true),Map.of("If-Match","\"2\"")),400,"FORMATO_INVALIDO");
		erro(requisitar("PUT","/consultas/"+id+"/financeiro",a.token(),corpo("PENDENTE",new BigDecimal("10"),"PIX",true),Map.of("If-Match","\"2\"")),400,"FORMATO_INVALIDO");
		assertThat(requisitar("PUT","/consultas/"+id+"/financeiro",a.token(),corpo("COMPARECEU",new BigDecimal("-1"),"PIX",false),Map.of("If-Match","\"2\"")).statusCode()).isEqualTo(400);
		// isolamento: outra nutri não vê nem lança; o paciente não tem acesso
		erro(requisitar("GET","/consultas/"+id+"/financeiro",b.token(),null),404,"RECURSO_NAO_ENCONTRADO");
		erro(requisitar("PUT","/consultas/"+id+"/financeiro",b.token(),corpo("FALTOU",null,null,false),Map.of("If-Match","\"0\"")),404,"RECURSO_NAO_ENCONTRADO");
		assertThat(requisitar("GET","/consultas/"+id+"/financeiro",p.token(),null).statusCode()).isEqualTo(403);
		assertThat(requisitar("GET","/consultas/"+id+"/financeiro",null,null).statusCode()).isEqualTo(401);
		// a consulta que o paciente lê não traz dinheiro
		var minhas=json(requisitar("GET","/eu/consultas",p.token(),null)).path("itens").get(0);
		assertThat(minhas.has("valor")||minhas.has("formaPagamento")||minhas.has("pago")||minhas.has("comparecimento")).isFalse();
	}

	@Test void canceladaEEncerradaNaoRecebemLancamento() throws Exception {
		var a=cadastrarNutri();var p=pacienteVinculado(a);String id=agendar(a.token(),p.fichaId(),2);
		assertThat(requisitar("PATCH","/consultas/"+id,a.token(),Map.of("status","CANCELADA"),Map.of("If-Match","\"1\"")).statusCode()).isEqualTo(200);
		erro(requisitar("PUT","/consultas/"+id+"/financeiro",a.token(),corpo("FALTOU",null,null,false),Map.of("If-Match","\"0\"")),422,"CONSULTA_CANCELADA");
		String outra=agendar(a.token(),p.fichaId(),3);
		assertThat(requisitar("POST","/pacientes/"+p.fichaId()+"/encerrar-vinculo",a.token(),null).statusCode()).isEqualTo(200);
		erro(requisitar("PUT","/consultas/"+outra+"/financeiro",a.token(),corpo("COMPARECEU",new BigDecimal("100"),"PIX",true),Map.of("If-Match","\"0\"")),403,"ACESSO_NEGADO");
		assertThat(requisitar("GET","/consultas/"+outra+"/financeiro",a.token(),null).statusCode()).isEqualTo(200);
	}

	@Test void resumoDoPeriodoSomaRecebidoEAReceberSemContarCanceladas() throws Exception {
		var a=cadastrarNutri();var b=cadastrarNutri();var p=pacienteVinculado(a);
		String c1=agendar(a.token(),p.fichaId(),1),c2=agendar(a.token(),p.fichaId(),2),c3=agendar(a.token(),p.fichaId(),3),c4=agendar(a.token(),p.fichaId(),4);
		assertThat(requisitar("PUT","/consultas/"+c1+"/financeiro",a.token(),corpo("COMPARECEU",new BigDecimal("200"),"PIX",true),Map.of("If-Match","\"0\"")).statusCode()).isEqualTo(200);
		assertThat(requisitar("PUT","/consultas/"+c2+"/financeiro",a.token(),corpo("COMPARECEU",new BigDecimal("150.50"),null,false),Map.of("If-Match","\"0\"")).statusCode()).isEqualTo(200);
		assertThat(requisitar("PUT","/consultas/"+c3+"/financeiro",a.token(),corpo("FALTOU",null,null,false),Map.of("If-Match","\"0\"")).statusCode()).isEqualTo(200);
		assertThat(requisitar("PATCH","/consultas/"+c4,a.token(),Map.of("status","CANCELADA"),Map.of("If-Match","\"1\"")).statusCode()).isEqualTo(200);
		String de=Instant.now().minus(1,ChronoUnit.DAYS).toString(),ate=Instant.now().plus(10,ChronoUnit.DAYS).toString();
		var r=json(requisitar("GET","/eu/nutri/financeiro?de="+de+"&ate="+ate,a.token(),null));
		assertThat(r.path("consultas").asInt()).isEqualTo(3);assertThat(r.path("compareceram").asInt()).isEqualTo(2);assertThat(r.path("faltaram").asInt()).isEqualTo(1);assertThat(r.path("pendentes").asInt()).isZero();
		assertThat(r.path("recebido").decimalValue()).isEqualByComparingTo("200");assertThat(r.path("aReceber").decimalValue()).isEqualByComparingTo("150.50");
		assertThat(r.path("recebidoPorForma").path("PIX").decimalValue()).isEqualByComparingTo("200");
		// outra nutri vê só o dela; janela inválida e perfil errado
		assertThat(json(requisitar("GET","/eu/nutri/financeiro?de="+de+"&ate="+ate,b.token(),null)).path("consultas").asInt()).isZero();
		erro(requisitar("GET","/eu/nutri/financeiro?de="+ate+"&ate="+de,a.token(),null),400,"FORMATO_INVALIDO");
		assertThat(requisitar("GET","/eu/nutri/financeiro",p.token(),null).statusCode()).isEqualTo(403);
	}
}
