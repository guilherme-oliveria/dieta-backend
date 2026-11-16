package com.br.dietamais.questionarios;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.api.ExcecaoNegocio;
import com.br.dietamais.questionarios.api.*;
import com.br.dietamais.questionarios.dominio.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class DerivarAnamneseTest {
	final JsonMapper mapper=JsonMapper.builder().build();
	PerguntaQuestionario p(String codigo,TipoPergunta tipo){return new PerguntaQuestionario(UUID.randomUUID(),codigo,tipo,null,false,null,codigo);}
	@Test void calculaSomenteComEntradasCompletasSemInventarZero(){
		var s=p("PA_SISTOLICA",TipoPergunta.NUMERO);var d=p("PA_DIASTOLICA",TipoPergunta.NUMERO);
		var blocos=List.of(new BlocoQuestionario("Clínica",List.of(s,d)));
		assertThat(DerivarAnamnese.calcular(blocos,Map.of(s.id(),mapper.valueToTree(140))).pressaoArterial()).isNull();
		assertThat(DerivarAnamnese.calcular(blocos,Map.of(s.id(),mapper.valueToTree(140),d.id(),mapper.valueToTree(80))).pressaoArterial()).isEqualTo("HIPERTENSAO_SISTOLICA_ISOLADA");
		assertThat(DerivarAnamnese.calcular(blocos,Map.of()).salGramasPorPessoaDia()).isNull();
	}
	@Test void integraAsCincoCalculadorasEPermiteConsumoZero(){
		var valores=new LinkedHashMap<String,Object>();
		valores.put("ALCOOL_BEBIDA","Cerveja");valores.put("ALCOOL_UNIDADE","Latinha (355 ml)");
		valores.put("ALCOOL_QUANTIDADE",3);valores.put("ALCOOL_DIAS_SEMANA",3);
		valores.put("SAL_GRAMAS_MES",650);valores.put("SAL_PESSOAS",3);
		valores.put("OLEO_ML_MES",2000);valores.put("OLEO_PESSOAS",4);
		valores.put("ATIVIDADE_INICIO","23:30");valores.put("ATIVIDADE_FIM","00:15");
		var perguntas=new ArrayList<PerguntaQuestionario>();var respostas=new HashMap<UUID,JsonNode>();
		valores.forEach((codigo,v)->{var p=p(codigo,v instanceof Number?TipoPergunta.NUMERO:codigo.startsWith("ATIVIDADE")?TipoPergunta.HORARIO:TipoPergunta.ESCOLHA_UNICA);perguntas.add(p);respostas.put(p.id(),mapper.valueToTree(v));});
		var blocos=List.of(new BlocoQuestionario("Hábitos",perguntas));var r=DerivarAnamnese.calcular(blocos,respostas);
		assertThat(r.alcool().gramasEtanolPorSemana()).isEqualByComparingTo("127.80");
		assertThat(r.salGramasPorPessoaDia()).isEqualTo(7);assertThat(r.oleoMlPorPessoaDia()).isEqualTo(16);
		assertThat(r.oleoKcalPorPessoaDia()).isEqualTo(144);assertThat(r.atividadeDuracaoMinutos()).isEqualTo(45);
		respostas.put(perguntas.get(2).id(),mapper.valueToTree(0));
		assertThat(DerivarAnamnese.calcular(blocos,respostas).alcool().gramasEtanolPorSemana()).isZero();
	}
	@Test void rejeitaValoresClinicosInvalidosComCampo(){
		for(var entrada:Map.of("PA_SISTOLICA",0,"PA_DIASTOLICA",80.5,"SAL_PESSOAS",0,"OLEO_ML_MES",-1,"ALCOOL_DIAS_SEMANA",8).entrySet()){
			var p=p(entrada.getKey(),TipoPergunta.NUMERO);
			assertThatThrownBy(()->ValidadorResposta.validar(p,mapper.valueToTree(entrada.getValue()))).isInstanceOf(ExcecaoNegocio.class);
		}
	}
	@Test void ignoraRespostaDePerguntaOculta(){
		var escolha=new PerguntaQuestionario(UUID.randomUUID(),"Atividade?",TipoPergunta.ESCOLHA_UNICA,List.of("Sim","Não"),false,null);
		var inicio=p("ATIVIDADE_INICIO",TipoPergunta.HORARIO);
		var fim=new PerguntaQuestionario(UUID.randomUUID(),"Fim",TipoPergunta.HORARIO,null,false,new CondicaoPergunta(escolha.id(),OperadorCondicao.IGUAL,"Sim"),"ATIVIDADE_FIM");
		var r=DerivarAnamnese.calcular(List.of(new BlocoQuestionario("Treino",List.of(escolha,inicio,fim))),Map.of(escolha.id(),mapper.valueToTree("Não"),inicio.id(),mapper.valueToTree("08:00"),fim.id(),mapper.valueToTree("09:00")));
		assertThat(r.atividadeDuracaoMinutos()).isNull();
	}
	@Test void modeloRejeitaCodigoDuplicadoTipoErradoEOpcaoIncompativel(){
		var a=new PerguntaQuestionario(null,"Sistólica",TipoPergunta.NUMERO,null,false,null,"PA_SISTOLICA");
		var duplicada=new PerguntaQuestionario(null,"Outra sistólica",TipoPergunta.NUMERO,null,false,null,"PA_SISTOLICA");
		var errado=new PerguntaQuestionario(null,"Sistólica",TipoPergunta.TEXTO,null,false,null,"PA_SISTOLICA");
		var bebida=new PerguntaQuestionario(null,"Bebida",TipoPergunta.ESCOLHA_UNICA,List.of("Cerveja","Outra"),false,null,"ALCOOL_BEBIDA");
		for(var perguntas:List.of(List.of(a,duplicada),List.of(errado),List.of(bebida))){
			assertThatThrownBy(()->ValidadorModelo.normalizar(List.of(new BlocoQuestionario("Bloco",perguntas)),UUID::randomUUID)).isInstanceOf(ExcecaoNegocio.class);
		}
		var normal=ValidadorModelo.normalizar(List.of(new BlocoQuestionario("Bloco",List.of(a))),UUID::randomUUID);
		assertThat(normal.get(0).perguntas().get(0).codigo()).isEqualTo("PA_SISTOLICA");
	}
	@Test void somaBebidasDaPlanilhaSemOcultarParcelaIncompletaEDerivaSemana(){
		var valores=Map.<String,Object>of("ALCOOL_CERVEJA_UNIDADE","Latinha (355 ml)","ALCOOL_CERVEJA_QUANTIDADE",3,"ALCOOL_CERVEJA_DIAS_SEMANA",3,
			"ALCOOL_VINHO_UNIDADE","Taça média (120 ml)","ALCOOL_VINHO_QUANTIDADE",1,"ALCOOL_VINHO_DIAS_SEMANA",1,
			"ATIVIDADE_SEGUNDA_INICIO","23:30","ATIVIDADE_SEGUNDA_FIM","00:15");
		var perguntas=new ArrayList<PerguntaQuestionario>();var respostas=new HashMap<UUID,JsonNode>();
		valores.forEach((codigo,v)->{var p=p(codigo,CodigosAnamnese.tipo(codigo));perguntas.add(p);respostas.put(p.id(),mapper.valueToTree(v));});
		var blocos=List.of(new BlocoQuestionario("Hábitos",perguntas));
		var r=DerivarAnamnese.calcular(blocos,respostas);
		assertThat(r.alcool().gramasEtanolPorSemana()).isEqualByComparingTo("140.28");
		assertThat(r.atividadesDuracaoMinutos()).containsExactlyEntriesOf(Map.of("SEGUNDA",45));
		respostas.remove(perguntas.stream().filter(p->p.codigo().equals("ALCOOL_VINHO_UNIDADE")).findFirst().orElseThrow().id());
		assertThat(DerivarAnamnese.calcular(blocos,respostas).alcool()).isNull();
	}
}
