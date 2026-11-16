package com.br.dietamais.questionarios.dominio;

import com.br.dietamais.questionarios.api.*;
import java.math.BigDecimal;
import java.util.*;
import tools.jackson.databind.JsonNode;
import static com.br.dietamais.questionarios.dominio.anamnese.CalculadoraAnamnese.*;

/** Códigos estáveis: editar o texto não muda a unidade nem a conta. */
public final class CodigosAnamnese {
	private CodigosAnamnese(){}
	public static final Map<String,Bebida> BEBIDAS;
	public static final Map<String,UnidadeBebida> UNIDADES;
	static {
		var bebidas=new LinkedHashMap<String,Bebida>();
		String[] nomes={"Absinto","Cachaça","Cerveja","Ice","Rum ou gim","Vinho","Vodca","Uísque"};
		for(int i=0;i<nomes.length;i++)bebidas.put(nomes[i],Bebida.values()[i]);BEBIDAS=Collections.unmodifiableMap(bebidas);
		var unidades=new LinkedHashMap<String,UnidadeBebida>();
		String[] medidas={"Cálice","Caneca","Copo americano","Copo duplo","Dose","Garrafa grande","Garrafa long neck","Latinha","Taça média","Taça pequena","Tulipa","Tulipinha"};
		for(int i=0;i<medidas.length;i++){var u=UnidadeBebida.values()[i];unidades.put(medidas[i]+" ("+u.ml()+" ml)",u);}UNIDADES=Collections.unmodifiableMap(unidades);
	}
	private static final Set<String> NUMEROS=Set.of("PA_SISTOLICA","PA_DIASTOLICA","ALCOOL_QUANTIDADE","ALCOOL_DIAS_SEMANA","SAL_GRAMAS_MES","SAL_PESSOAS","OLEO_ML_MES","OLEO_PESSOAS");
	public static final List<String> DIAS=List.of("SEGUNDA","TERCA","QUARTA","QUINTA","SEXTA","SABADO","DOMINGO");
	public static String prefixo(Bebida bebida){return "ALCOOL_"+bebida.name()+"_";}
	public static String prefixoAtividade(String dia){return "ATIVIDADE_"+dia+"_";}
	private static boolean bebidaCampo(String codigo,String campo){return Arrays.stream(Bebida.values()).anyMatch(b->codigo.equals(prefixo(b)+campo));}
	private static boolean atividadeCampo(String codigo,String campo){return DIAS.stream().anyMatch(d->codigo.equals(prefixoAtividade(d)+campo));}
	public static TipoPergunta tipo(String codigo){
		if(codigo==null)return null;
		if(NUMEROS.contains(codigo))return TipoPergunta.NUMERO;
		if(bebidaCampo(codigo,"QUANTIDADE")||bebidaCampo(codigo,"DIAS_SEMANA"))return TipoPergunta.NUMERO;
		if(bebidaCampo(codigo,"UNIDADE"))return TipoPergunta.ESCOLHA_UNICA;
		if(atividadeCampo(codigo,"INICIO")||atividadeCampo(codigo,"FIM"))return TipoPergunta.HORARIO;
		return switch(codigo){case "ALCOOL_BEBIDA","ALCOOL_UNIDADE"->TipoPergunta.ESCOLHA_UNICA;case "ATIVIDADE_INICIO","ATIVIDADE_FIM"->TipoPergunta.HORARIO;default->null;};
	}
	public static List<String> opcoes(String codigo){
		return "ALCOOL_BEBIDA".equals(codigo)?List.copyOf(BEBIDAS.keySet()):codigo!=null&&("ALCOOL_UNIDADE".equals(codigo)||bebidaCampo(codigo,"UNIDADE"))?List.copyOf(UNIDADES.keySet()):null;
	}
	public static boolean valorValido(String codigo,JsonNode v){
		if(codigo==null||tipo(codigo)!=TipoPergunta.NUMERO)return true;
		if(!v.isNumber())return false;
		var numero=new BigDecimal(v.asString());
		if(numero.signum()<0)return false;
		if(codigo.startsWith("PA_")||codigo.endsWith("_PESSOAS")){
			if(numero.signum()==0||numero.stripTrailingZeros().scale()>0)return false;
			return numero.compareTo(BigDecimal.valueOf(codigo.startsWith("PA_")?1000:1_000_000))<=0;
		}
		return !codigo.endsWith("_DIAS_SEMANA")||numero.compareTo(BigDecimal.valueOf(7))<=0;
	}
}
