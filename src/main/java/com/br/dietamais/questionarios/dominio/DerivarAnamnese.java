package com.br.dietamais.questionarios.dominio;

import com.br.dietamais.questionarios.api.*;
import com.br.dietamais.questionarios.dominio.anamnese.CalculadoraAnamnese;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;
import tools.jackson.databind.JsonNode;

/** RN-CAL-04: somente perguntas visíveis e entradas completas alimentam o cálculo. */
public final class DerivarAnamnese {
	private DerivarAnamnese(){}
	public static AnamneseDerivada calcular(List<BlocoQuestionario> blocos,Map<UUID,JsonNode> respostas){
		var valores=new HashMap<String,JsonNode>();
		var perguntas=ValidadorResposta.indice(blocos);
		for(var p:perguntas.values()){
			var v=respostas.get(p.id());
			if(p.codigo()!=null&&v!=null&&!v.isNull()&&visivel(p,perguntas,respostas,new HashSet<>()))valores.put(p.codigo(),v);
		}
		String pressao=null;AnamneseDerivada.Alcool alcool=null;Integer sal=null,oleo=null,kcal=null,duracao=null;
		if(completos(valores,"PA_SISTOLICA","PA_DIASTOLICA"))pressao=CalculadoraAnamnese.classificarPressao(inteiro(valores,"PA_SISTOLICA"),inteiro(valores,"PA_DIASTOLICA")).name();
		alcool=alcool(valores);
		if(completos(valores,"SAL_GRAMAS_MES","SAL_PESSOAS"))sal=CalculadoraAnamnese.salGramasPorPessoaDia(numero(valores,"SAL_GRAMAS_MES"),inteiro(valores,"SAL_PESSOAS"));
		if(completos(valores,"OLEO_ML_MES","OLEO_PESSOAS")){
			oleo=CalculadoraAnamnese.oleoMlPorPessoaDia(numero(valores,"OLEO_ML_MES"),inteiro(valores,"OLEO_PESSOAS"));
			kcal=CalculadoraAnamnese.oleoKcalPorPessoaDia(numero(valores,"OLEO_ML_MES"),inteiro(valores,"OLEO_PESSOAS"));
		}
		if(completos(valores,"ATIVIDADE_INICIO","ATIVIDADE_FIM"))duracao=CalculadoraAnamnese.duracaoMinutos(LocalTime.parse(valores.get("ATIVIDADE_INICIO").asString()),LocalTime.parse(valores.get("ATIVIDADE_FIM").asString()));
		var atividades=new LinkedHashMap<String,Integer>();
		for(var dia:CodigosAnamnese.DIAS){
			var prefixo=CodigosAnamnese.prefixoAtividade(dia);
			if(completos(valores,prefixo+"INICIO",prefixo+"FIM"))atividades.put(dia,CalculadoraAnamnese.duracaoMinutos(LocalTime.parse(valores.get(prefixo+"INICIO").asString()),LocalTime.parse(valores.get(prefixo+"FIM").asString())));
		}
		return new AnamneseDerivada(pressao,alcool,sal,oleo,kcal,duracao,Map.copyOf(atividades));
	}
	private static AnamneseDerivada.Alcool alcool(Map<String,JsonNode> valores){
		var parcelas=new ArrayList<CalculadoraAnamnese.Alcool>();
		String[] genericos={"ALCOOL_BEBIDA","ALCOOL_UNIDADE","ALCOOL_QUANTIDADE","ALCOOL_DIAS_SEMANA"};
		if(Arrays.stream(genericos).anyMatch(valores::containsKey)){
			if(!completos(valores,genericos))return null;
			parcelas.add(CalculadoraAnamnese.alcool(CodigosAnamnese.BEBIDAS.get(valores.get("ALCOOL_BEBIDA").asString()),CodigosAnamnese.UNIDADES.get(valores.get("ALCOOL_UNIDADE").asString()),numero(valores,"ALCOOL_QUANTIDADE"),numero(valores,"ALCOOL_DIAS_SEMANA")));
		}
		for(var bebida:CalculadoraAnamnese.Bebida.values()){
			var p=CodigosAnamnese.prefixo(bebida);String[] campos={p+"UNIDADE",p+"QUANTIDADE",p+"DIAS_SEMANA"};
			if(Arrays.stream(campos).noneMatch(valores::containsKey))continue;
			if(!completos(valores,campos))return null;
			parcelas.add(CalculadoraAnamnese.alcool(bebida,CodigosAnamnese.UNIDADES.get(valores.get(p+"UNIDADE").asString()),numero(valores,p+"QUANTIDADE"),numero(valores,p+"DIAS_SEMANA")));
		}
		if(parcelas.isEmpty())return null;
		var a=CalculadoraAnamnese.somar(parcelas);
		return new AnamneseDerivada.Alcool(a.mlPorSemana(),a.gramasEtanolPorSemana(),a.kcalPorSemana());
	}
	private static boolean visivel(PerguntaQuestionario p,Map<UUID,PerguntaQuestionario> perguntas,Map<UUID,JsonNode> respostas,Set<UUID> visitadas){
		if(!visitadas.add(p.id())||!ValidadorResposta.visivel(p,respostas))return false;
		if(p.condicao()==null)return true;
		var alvo=perguntas.get(p.condicao().perguntaId());
		return alvo!=null&&visivel(alvo,perguntas,respostas,visitadas);
	}
	private static boolean completos(Map<String,JsonNode> valores,String... codigos){return Arrays.stream(codigos).allMatch(valores::containsKey);}
	private static BigDecimal numero(Map<String,JsonNode> valores,String codigo){return new BigDecimal(valores.get(codigo).asString());}
	private static int inteiro(Map<String,JsonNode> valores,String codigo){return numero(valores,codigo).intValueExact();}
}
