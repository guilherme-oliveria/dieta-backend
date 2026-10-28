package com.br.dietamais.questionarios.dominio;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.questionarios.api.*;
import java.util.*;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/** Valor de resposta por tipo (RN-ANA-02) e regra de exibição das perguntas condicionais. */
public final class ValidadorResposta {
	private static final Pattern HORA=Pattern.compile("([01][0-9]|2[0-3]):[0-5][0-9]");
	private ValidadorResposta(){}

	public static Map<UUID,PerguntaQuestionario> indice(List<BlocoQuestionario> blocos){
		var m=new LinkedHashMap<UUID,PerguntaQuestionario>();
		for(var b:blocos)for(var p:b.perguntas())m.put(p.id(),p);
		return m;
	}

	/** Lança 400 {@code FORMATO_INVALIDO} apontando a pergunta cujo valor não cabe no tipo. */
	public static void validar(PerguntaQuestionario p,JsonNode v){
		boolean ok=switch(p.tipo()){
			case TEXTO->v.isString()&&!v.asString().isBlank()&&v.asString().length()<=2000;
			case NUMERO->v.isNumber()&&Double.isFinite(v.asDouble())&&Math.abs(v.asDouble())<=1_000_000_000d;
			case ESCOLHA_UNICA->v.isString()&&p.opcoes().contains(v.asString());
			case MULTIPLA_ESCOLHA->multipla(p,v);
			case ESCALA_1_5->v.isIntegralNumber()&&v.asInt()>=1&&v.asInt()<=5;
			case HORARIO->v.isString()&&HORA.matcher(v.asString()).matches();
			case LISTA_POR_REFEICAO->lista(v);
		};
		if(!ok)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-ANA-02","Resposta inválida para o tipo da pergunta.",
			List.of(new ErroCampoResposta(p.id().toString(),"FORMATO_INVALIDO","Resposta inválida para o tipo da pergunta.")),null);
	}
	private static boolean multipla(PerguntaQuestionario p,JsonNode v){
		if(!v.isArray()||v.isEmpty()||v.size()>p.opcoes().size())return false;
		var vistos=new HashSet<String>();
		for(var e:v)if(!e.isString()||!p.opcoes().contains(e.asString())||!vistos.add(e.asString()))return false;
		return true;
	}
	private static boolean lista(JsonNode v){
		if(!v.isArray()||v.isEmpty()||v.size()>12)return false;
		for(var e:v){
			if(!e.isObject())return false;
			var r=e.get("refeicao");var d=e.get("descricao");var h=e.get("horario");
			if(r==null||!r.isString()||r.asString().isBlank()||r.asString().length()>60)return false;
			if(d==null||!d.isString()||d.asString().isBlank()||d.asString().length()>500)return false;
			if(h!=null&&!h.isNull()&&!(h.isString()&&HORA.matcher(h.asString()).matches()))return false;
			for(var campo:e.propertyNames())if(!Set.of("refeicao","descricao","horario").contains(campo))return false;
		}
		return true;
	}

	/** Uma pergunta condicional só vale quando a de referência satisfaz o operador; sem resposta, IGUAL e CONTEM falham e DIFERENTE passa. */
	public static boolean visivel(PerguntaQuestionario p,Map<UUID,JsonNode> respostas){
		var c=p.condicao();if(c==null)return true;
		var r=respostas.get(c.perguntaId());
		boolean igual=r!=null&&(r.isString()?r.asString().equals(c.valor()):r.isArray()&&r.size()==1&&r.get(0).asString().equals(c.valor()));
		boolean contem=r!=null&&r.isArray()&&contemValor(r,c.valor());
		return switch(c.operador()){case IGUAL->igual;case DIFERENTE->!igual;case CONTEM->contem;};
	}
	private static boolean contemValor(JsonNode arr,String valor){for(var e:arr)if(e.isString()&&e.asString().equals(valor))return true;return false;}
}
