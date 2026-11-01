package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.TesteComPaciente;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** BE-41: o servidor calcula e grava os resultados da planilha ao salvar (REST e /sync usam o mesmo caso de uso). */
class AvaliacaoAmpliadaApiTest extends TesteComPaciente {
	Map<String,Object> corpo(Map<String,Object> extra){
		var m=new HashMap<String,Object>();m.put("dataHora",Instant.now().minusSeconds(60).toString());m.put("pesoKg",102.5);m.put("estaturaM",1.93);
		m.put("perimetros",Map.of("cintura",100));
		m.put("dobras",Map.of("triceps",20,"peitoral",25,"axilarMedia",28,"subescapular",30,"abdominal",40,"suprailiaca",32,"coxa",24));
		m.put("complementares",Map.of("punhoCm",18,"percentualGorduraDesejado",18));
		m.putAll(extra);return m;
	}
	PacienteTeste paciente(ContaTeste nutri,String sexo,LocalDate nascimento) throws Exception {
		var p=pacienteVinculado(nutri);
		jdbc.update("update ficha_paciente set sexo=?,data_nascimento=? where vinculo_id=?::uuid",sexo,nascimento==null?null:java.sql.Date.valueOf(nascimento),p.vinculoId());
		return p;
	}
	JsonNode criar(ContaTeste nutri,PacienteTeste p,Map<String,Object> corpo) throws Exception {
		var r=requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",nutri.token(),corpo);assertThat(r.statusCode()).as(r.body()).isEqualTo(201);return json(r);
	}

	@Test void calculaEGravaOsResultadosDaPlanilha() throws Exception {
		var a=cadastrarNutri();var p=paciente(a,"MASCULINO",LocalDate.now().minusYears(29).minusMonths(6));
		var av=criar(a,p,corpo(Map.of()));
		var r=av.path("resultados");
		assertThat(av.path("faixaEtaria").asString()).isEqualTo("ADULTO");
		assertThat(av.path("protocolo").path("dobras").asString()).isEqualTo("SETE");assertThat(av.path("protocolo").path("versaoReferencia").asString()).isEqualTo("adulto-1.0.0");
		assertThat(r.path("idadeAnos").asInt()).isEqualTo(29);assertThat(r.path("classificacaoImc").asString()).isEqualTo("SP");assertThat(r.path("riscoComorbidades").asString()).isEqualTo("A");
		assertThat(r.path("densidadeCorporal").decimalValue()).isEqualByComparingTo("1.0389");assertThat(r.path("percentualGordura").decimalValue()).isEqualByComparingTo("26.48");
		assertThat(r.path("classificacaoPercentualGordura").asString()).isEqualTo("ALTO");assertThat(r.path("pesoParaPercentualDesejadoKg").decimalValue()).isEqualByComparingTo("91.9");
		assertThat(r.path("motivosNulos").path("pesoOsseoKg").asString()).isEqualTo("FALTA_DIAMETRO_PUNHO");
		assertThat(r.path("textoResumo").asString()).startsWith("O avaliado apresenta sobrepeso");
		var lido=json(requisitar("GET","/avaliacoes/"+av.path("id").asString(),a.token(),null));
		assertThat(lido.path("resultados").path("percentualGordura").decimalValue()).isEqualByComparingTo("26.48");
		assertThat(lido.path("complementares").path("punhoCm").decimalValue()).isEqualByComparingTo("18");
	}
	@Test void protocoloEscolhidoVaiParaOCalculoEAVersaoDaReferenciaEDoServidor() throws Exception {
		var a=cadastrarNutri();var p=paciente(a,"MASCULINO",LocalDate.now().minusYears(29).minusMonths(6));
		var av=criar(a,p,corpo(Map.of("protocolo",Map.of("dobras","TRES","massa","MAGRO","versaoReferencia","inventada"))));
		assertThat(av.path("protocolo").path("dobras").asString()).isEqualTo("TRES");assertThat(av.path("protocolo").path("versaoReferencia").asString()).isEqualTo("adulto-1.0.0");
		assertThat(av.path("resultados").path("percentualGordura").decimalValue()).isNotEqualByComparingTo("26.48");
	}
	@Test void semSexoOuNascimentoNaoFalhaEExplicaOMotivo() throws Exception {
		var a=cadastrarNutri();
		var semSexo=criar(a,paciente(a,null,LocalDate.now().minusYears(30)),corpo(Map.of()));
		assertThat(semSexo.path("resultados").path("motivosNulos").path("resultados").asString()).isEqualTo("FALTA_SEXO");assertThat(semSexo.path("resultados").has("percentualGordura")).isFalse();
		var semNasc=criar(a,paciente(a,"FEMININO",null),corpo(Map.of()));
		assertThat(semNasc.path("resultados").path("motivosNulos").path("resultados").asString()).isEqualTo("FALTA_NASCIMENTO");
		assertThat(semNasc.path("imc").decimalValue()).isEqualByComparingTo("27.52");
	}
	@Test void pacienteSoVeASecaoCompartilhadaEOutraNutriNaoVeNada() throws Exception {
		var a=cadastrarNutri();var p=paciente(a,"MASCULINO",LocalDate.now().minusYears(29).minusMonths(6));var b=cadastrarNutri();
		var av=criar(a,p,corpo(Map.of("compartilhamento",Map.of("composicao",false,"resumo",false),"parecer","Texto interno")));
		var meus=json(requisitar("GET","/eu/avaliacoes",p.token(),null)).path("itens");assertThat(meus).hasSize(1);
		var visto=meus.get(0);
		assertThat(visto.path("resultados").has("percentualGordura")).isFalse();assertThat(visto.path("resultados").has("textoResumo")).isFalse();assertThat(visto.path("resultados").has("motivosNulos")).isFalse();
		assertThat(visto.path("resultados").path("classificacaoImc").asString()).isEqualTo("SP");assertThat(visto.path("dobras").path("triceps").decimalValue()).isEqualByComparingTo("20");
		assertThat(visto.path("parecer").isNull()).isTrue();
		assertThat(requisitar("GET","/avaliacoes/"+av.path("id").asString(),b.token(),null).statusCode()).isEqualTo(404);
	}
	@Test void evolucaoTemSerieDeDobras() throws Exception {
		var a=cadastrarNutri();var p=paciente(a,"MASCULINO",LocalDate.now().minusYears(29).minusMonths(6));criar(a,p,corpo(Map.of()));
		var series=json(requisitar("GET","/pacientes/"+p.fichaId()+"/evolucao",a.token(),null)).path("series");
		boolean achou=false;for(var s:series)if(s.path("medida").asString().equals("SOMATORIO_DOBRAS")){achou=true;assertThat(s.path("pontos").get(0).path("valor").decimalValue()).isEqualByComparingTo("199");}
		assertThat(achou).isTrue();
	}
	@Test void valoresComplementaresForaDoLimiteSaoRejeitados() throws Exception {
		var a=cadastrarNutri();var p=paciente(a,"MASCULINO",LocalDate.now().minusYears(29));
		var r=requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),corpo(Map.of("complementares",Map.of("punhoCm",2))));
		assertThat(r.statusCode()).isEqualTo(422);assertThat(json(r).path("codigo").asString()).isEqualTo("VALOR_FORA_DO_INTERVALO");
	}
	@Test void energiaEscolhidaVaiParaOCalculoEFicaGravada() throws Exception {
		var a=cadastrarNutri();var p=paciente(a,"MASCULINO",LocalDate.now().minusYears(29).minusMonths(6));
		var compl=Map.of("punhoCm",18,"nivelAtividade","MODERADO");
		var av=criar(a,p,corpo(Map.of("complementares",compl,"energia",Map.of("metodo","FAO_2004","carboidrato","FAIXA_ESPORTIVA","dislipidemico",true,"colesterolTotal",230))));
		var e=av.path("energiaResultado");
		assertThat(e.path("metodo").asString()).isEqualTo("FAO_2004");assertThat(e.path("gebKcal").decimalValue()).isEqualByComparingTo("2235.54");
		assertThat(e.path("getKcal").decimalValue()).isEqualByComparingTo("3934.55");assertThat(e.path("carboidrato").path("percentualMinimo").decimalValue()).isEqualByComparingTo("50");
		assertThat(e.path("colesterolMaximoMg").decimalValue()).isEqualByComparingTo("200");assertThat(av.path("energia").path("carboidrato").asString()).isEqualTo("FAIXA_ESPORTIVA");
		var lido=json(requisitar("GET","/avaliacoes/"+av.path("id").asString(),a.token(),null));
		assertThat(lido.path("energiaResultado").path("vetKcal").decimalValue()).isEqualByComparingTo("3934.55");
	}
	@Test void semEscolhaAAvaliacaoNaoTemEnergia() throws Exception {
		var a=cadastrarNutri();var p=paciente(a,"MASCULINO",LocalDate.now().minusYears(29));
		var av=criar(a,p,corpo(Map.of()));
		assertThat(av.has("energiaResultado")).isFalse();assertThat(av.has("energia")).isFalse();
	}
	@Test void pacienteSoVeAEnergiaSeNecessidadesForCompartilhada() throws Exception {
		var a=cadastrarNutri();var p=paciente(a,"MASCULINO",LocalDate.now().minusYears(29));
		criar(a,p,corpo(Map.of("complementares",Map.of("nivelAtividade","LEVE"),"energia",Map.of("metodo","FAO_2004"),"compartilhamento",Map.of("necessidades",false))));
		var visto=json(requisitar("GET","/eu/avaliacoes",p.token(),null)).path("itens").get(0);
		assertThat(visto.has("energiaResultado")).isFalse();assertThat(visto.has("energia")).isFalse();
	}
	@Test void rotinaMetComAtividadeDesconhecidaERejeitada() throws Exception {
		var a=cadastrarNutri();var p=paciente(a,"MASCULINO",LocalDate.now().minusYears(29));
		var rotina=Map.of("horasSono",8,"horasTrabalho",8,"intensidadeTrabalho","LEVE","atividades",List.of(Map.of("codigo","inexistente","minutos",60)));
		var r=requisitar("POST","/pacientes/"+p.fichaId()+"/avaliacoes",a.token(),corpo(Map.of("energia",Map.of("metodo","MET","rotinaMet",rotina))));
		assertThat(r.statusCode()).isEqualTo(400);assertThat(json(r).path("codigo").asString()).isEqualTo("FORMATO_INVALIDO");
	}
	@Test void listaAtividadesMetEMetasDriParaOApp() throws Exception {
		var a=cadastrarNutri();
		var mets=json(requisitar("GET","/referencias/atividades-met",a.token(),null)).path("itens");
		assertThat(mets.size()).isGreaterThan(100);assertThat(mets.get(0).path("codigo").asString()).isEqualTo("alpinismo");
		var dri=json(requisitar("GET","/referencias/dri?sexo=feminino&idadeMeses=360&lactante=true",a.token(),null));
		assertThat(dri.path("lactante").asBoolean()).isTrue();assertThat(dri.path("itens").size()).isGreaterThan(20);
		assertThat(requisitar("GET","/referencias/dri?sexo=outro&idadeMeses=360",a.token(),null).statusCode()).isEqualTo(400);
	}
	@Test void buscaAlimentosNaTacoComFonte() throws Exception {
		var a=cadastrarNutri();
		var r=json(requisitar("GET","/referencias/taco?busca=ARROZ%20integral&limite=5",a.token(),null));
		assertThat(r.path("fonte").asString()).contains("TACO");assertThat(r.path("categorias").size()).isEqualTo(15);
		assertThat(r.path("itens").size()).isEqualTo(2);assertThat(r.path("itens").get(0).path("nome").asString()).isEqualTo("Arroz, integral, cozido");
		var arroz=json(requisitar("GET","/referencias/taco/1",a.token(),null));
		assertThat(arroz.path("nutrientes").path("energia_kcal").decimalValue()).isEqualByComparingTo("124");
		assertThat(arroz.path("nutrientes").has("colesterol_mg")).isFalse();
		assertThat(arroz.path("tracos").toString()).contains("riboflavina_mg");
		assertThat(requisitar("GET","/referencias/taco/9999",a.token(),null).statusCode()).isEqualTo(404);
		assertThat(requisitar("GET","/referencias/taco?limite=0",a.token(),null).statusCode()).isEqualTo(400);
	}
	@Test void consultaCurvasDaOms() throws Exception {
		var a=cadastrarNutri();
		var pi=json(requisitar("GET","/referencias/oms?indicador=PESO_IDADE&sexo=feminino&idadeMeses=0",a.token(),null)).path("curva");
		assertThat(pi.path("percentis").size()).isEqualTo(8);assertThat(pi.path("percentis").get(4).decimalValue()).isEqualByComparingTo("3.2");
		var imc=json(requisitar("GET","/referencias/oms?indicador=IMC_IDADE&sexo=masculino&idadeMeses=150",a.token(),null)).path("curva");
		assertThat(imc.path("idadeMeses").asInt()).isEqualTo(144);assertThat(imc.path("percentis").get(4).decimalValue()).isEqualByComparingTo("17.5");
		assertThat(requisitar("GET","/referencias/oms?indicador=PESO_IDADE&sexo=feminino&idadeMeses=130",a.token(),null).statusCode()).isEqualTo(404);
		assertThat(requisitar("GET","/referencias/oms?indicador=IMC_IDADE&sexo=feminino&idadeMeses=216",a.token(),null).statusCode()).isEqualTo(404);
		assertThat(requisitar("GET","/referencias/oms?indicador=X&sexo=feminino&idadeMeses=1",a.token(),null).statusCode()).isEqualTo(400);
	}
}
