package com.br.dietamais.dietas.dominio;

import com.br.dietamais.dietas.api.*;
import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Micronutrientes da dieta (RN-DIE-13) e adequação do dia às DRI do paciente. Os códigos de `GET /referencias/porcoes` e de `GET /referencias/dri` são
 * diferentes, então a tabela abaixo liga um ao outro. Limites de classe (ABAIXO &lt; 90%, ACIMA &gt; 110%) são valores provisórios.
 */
public final class AdequacaoDri {
	private AdequacaoDri(){}
	static final BigDecimal LIMITE_ABAIXO=new BigDecimal("90"),LIMITE_ACIMA=new BigDecimal("110"),CEM=new BigDecimal("100");
	/** Códigos que já têm campo próprio no total (energia, carboidrato, proteína, lipídio). */
	public static final Set<String> MACROS=Set.of("energia","gli","pro","lip");
	private record Par(String dri,BigDecimal fator){}
	private static final Map<String,Par> DRI=new LinkedHashMap<>();
	static{
		BigDecimal um=BigDecimal.ONE;
		String[][] c={{"fib","fibras"},{"na","sodio"},{"k","potassio"},{"ca","calcio"},{"cu","cobre"},{"i","iodo"},{"mg","magnesio"},{"mn","manganes"},{"p","fosforo"},{"se","selenio"},{"zn","zinco"},{"fe","ferro"},
			{"fol","vitamina_b9"},{"nia","vitamina_b3"},{"pan","vitamina_b5"},{"b2","vitamina_b2"},{"b1","vitamina_b1"},{"ret","vitamina_a"},{"b6","vitamina_b6"},{"b12","vitamina_b12"},{"c","vitamina_c"},{"d","vitamina_d"},{"e","vitamina_e"}};
		for(var p:c)DRI.put(p[0],new Par(p[1],um));
		DRI.put("agua",new Par("agua",new BigDecimal("0.001")));
	}
	/** O que um item que conta nos totais informa dos micronutrientes; item à vontade e texto livre não contam. */
	public record Contribuicao(Map<String,BigDecimal> valores) {}
	public record Soma(Map<String,BigDecimal> micronutrientes,List<String> incompleto) {}
	/** Soma dos itens que informam o nutriente, sem arredondar antes; o total fica com 2 casas. Sem nenhum micronutriente informado devolve nulos. */
	public static Soma somar(Collection<Contribuicao> itens){
		var soma=new LinkedHashMap<String,BigDecimal>();
		for(var cod:PorcoesGrupos.UNIDADES.keySet()){
			if(MACROS.contains(cod))continue;
			BigDecimal total=null;
			for(var i:itens){var v=i.valores().get(cod);if(v!=null)total=total==null?v:total.add(v);}
			if(total!=null)soma.put(cod,total.setScale(2,RoundingMode.HALF_UP));
		}
		if(soma.isEmpty())return new Soma(null,null);
		var faltantes=new ArrayList<String>();
		for(var cod:soma.keySet())if(itens.stream().anyMatch(i->!i.valores().containsKey(cod)))faltantes.add(cod);
		return new Soma(Collections.unmodifiableMap(soma),faltantes);
	}
	/** Total do dia contra as metas do perfil; só entram nutrientes que a dieta informa e que têm meta. */
	public static List<AdequacaoNutrienteResposta> adequacao(TotaisNutricionaisResposta dia,List<ValorDri> metas){
		var porCodigo=new HashMap<String,ValorDri>();metas.forEach(m->porCodigo.put(m.nutriente(),m));
		var lista=new ArrayList<AdequacaoNutrienteResposta>();
		var proteina=porCodigo.get("proteina");
		if(proteina!=null)lista.add(linha("proteina",dia.proteinas(),proteina.valor()));
		var micros=dia.micronutrientes();
		if(micros!=null)for(var e:DRI.entrySet()){
			var total=micros.get(e.getKey());var meta=porCodigo.get(e.getValue().dri());
			if(total!=null&&meta!=null)lista.add(linha(e.getValue().dri(),total.multiply(e.getValue().fator()),meta.valor()));
		}
		return lista;
	}
	private static AdequacaoNutrienteResposta linha(String nutriente,BigDecimal total,BigDecimal meta){
		var t=total.setScale(total.scale()>2?4:2,RoundingMode.HALF_UP);
		var pct=meta.signum()==0?BigDecimal.ZERO:total.multiply(CEM).divide(meta,2,RoundingMode.HALF_UP);
		var classe=pct.compareTo(LIMITE_ABAIXO)<0?"ABAIXO":pct.compareTo(LIMITE_ACIMA)>0?"ACIMA":"ADEQUADO";
		return new AdequacaoNutrienteResposta(nutriente,t,meta,pct,classe);
	}
}
