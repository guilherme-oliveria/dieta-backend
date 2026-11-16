package com.br.dietamais.questionarios.dominio.anamnese;

import java.math.*;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

/**
 * Porta das contas da anamnese da planilha da Nut. Luciana (plano das planilhas, onda 23): classificação da pressão arterial,
 * consumo de álcool, sal e óleo por pessoa e duração do exercício. Funções puras com {@link BigDecimal}; intermediários sem
 * arredondar, resultados com 2 casas em HALF_UP. RN-CAL-01, RN-CAL-06.
 */
public final class CalculadoraAnamnese {
	private static final BigDecimal DENSIDADE_ETANOL=new BigDecimal("0.8"),SETE=BigDecimal.valueOf(7),TRINTA=BigDecimal.valueOf(30),NOVE=BigDecimal.valueOf(9);
	private CalculadoraAnamnese(){}

	/** Classes da pressão arterial (CLASS_PA da planilha, diretriz brasileira de hipertensão). */
	public enum ClassePressao {
		OTIMA,NORMAL,LIMITROFE,HIPERTENSAO_1,HIPERTENSAO_2,HIPERTENSAO_3,HIPERTENSAO_SISTOLICA_ISOLADA
	}

	/** Bebidas e o teor alcoólico usado pela planilha (fração de etanol em volume). */
	public enum Bebida {
		ABSINTO("0.54"),CACHACA("0.44"),CERVEJA("0.05"),ICE("0.055"),RUM_GIM("0.43"),VINHO("0.13"),VODCA("0.35"),UISQUE("0.45");
		private final BigDecimal teor;
		Bebida(String teor){this.teor=new BigDecimal(teor);}
		public BigDecimal teor(){return teor;}
	}

	/** Medidas da planilha, em ml por unidade. */
	public enum UnidadeBebida {
		CALICE(80),CANECA(450),COPO_AMERICANO(140),COPO_DUPLO(200),DOSE(50),GARRAFA_GRANDE(600),GARRAFA_LONG_NECK(355),LATINHA(355),
		TACA_MEDIA(120),TACA_PEQUENA(80),TULIPA(280),TULIPINHA(180);
		private final int ml;
		UnidadeBebida(int ml){this.ml=ml;}
		public int ml(){return ml;}
	}

	public record Alcool(BigDecimal mlPorSemana,BigDecimal gramasEtanolPorSemana,BigDecimal kcalPorSemana){}

	/**
	 * Pressão arterial em mmHg. Vale a pior das duas classes (sistólica e diastólica); com sistólica a partir de 140 e diastólica
	 * abaixo de 90 é hipertensão sistólica isolada. Corrige a macro da planilha, que só chegava a esse ramo com 180 ou mais.
	 * Sem um dos valores devolve {@code null} (RN-CAL-04).
	 */
	public static ClassePressao classificarPressao(Integer sistolica,Integer diastolica){
		if(sistolica==null||diastolica==null)return null;
		if(sistolica<=0||diastolica<=0)throw new IllegalArgumentException("pressão deve ser positiva");
		if(sistolica>=140&&diastolica<90)return ClassePressao.HIPERTENSAO_SISTOLICA_ISOLADA;
		int s=sistolica<120?0:sistolica<130?1:sistolica<140?2:sistolica<160?3:sistolica<180?4:5;
		int d=diastolica<80?0:diastolica<85?1:diastolica<90?2:diastolica<100?3:diastolica<110?4:5;
		return ClassePressao.values()[Math.max(s,d)];
	}

	/** ml/semana = vezes por semana × quantidade × ml da unidade; kcal/semana = ml × 0,8 × 7 × teor (coluna da planilha). */
	public static Alcool alcool(Bebida bebida,UnidadeBebida unidade,BigDecimal quantidade,BigDecimal vezesPorSemana){
		if(quantidade.signum()<0||vezesPorSemana.signum()<0)throw new IllegalArgumentException("valores negativos");
		var ml=vezesPorSemana.multiply(quantidade).multiply(BigDecimal.valueOf(unidade.ml()));
		var gramas=ml.multiply(DENSIDADE_ETANOL).multiply(bebida.teor());
		var kcal=ml.multiply(DENSIDADE_ETANOL).multiply(SETE).multiply(bebida.teor());
		return new Alcool(casas(ml),casas(gramas),casas(kcal));
	}

	/** Soma de várias bebidas. */
	public static Alcool somar(List<Alcool> itens){
		var ml=BigDecimal.ZERO;var g=BigDecimal.ZERO;var k=BigDecimal.ZERO;
		for(var a:itens){ml=ml.add(a.mlPorSemana());g=g.add(a.gramasEtanolPorSemana());k=k.add(a.kcalPorSemana());}
		return new Alcool(casas(ml),casas(g),casas(k));
	}

	/** Sal em g/pessoa/dia: INT(g no mês ÷ pessoas ÷ 30). Referência da OMS: até 5 g. */
	public static int salGramasPorPessoaDia(BigDecimal gramasNoMes,int pessoas){
		return porPessoaDia(gramasNoMes,pessoas);
	}
	/** Óleo em ml/pessoa/dia: INT(ml no mês ÷ pessoas ÷ 30). */
	public static int oleoMlPorPessoaDia(BigDecimal mlNoMes,int pessoas){
		return porPessoaDia(mlNoMes,pessoas);
	}
	/** kcal do óleo: INT(ml/pessoa/dia × 9). */
	public static int oleoKcalPorPessoaDia(BigDecimal mlNoMes,int pessoas){
		return BigDecimal.valueOf(oleoMlPorPessoaDia(mlNoMes,pessoas)).multiply(NOVE).intValue();
	}
	private static int porPessoaDia(BigDecimal noMes,int pessoas){
		if(pessoas<=0)throw new IllegalArgumentException("pessoas deve ser positivo");
		if(noMes.signum()<0)throw new IllegalArgumentException("quantidade negativa");
		return noMes.divide(BigDecimal.valueOf(pessoas),MathContext.DECIMAL128).divide(TRINTA,MathContext.DECIMAL128).setScale(0,RoundingMode.DOWN).intValue();
	}

	/** Duração do exercício em minutos; se o fim for antes do início, passou da meia-noite. */
	public static int duracaoMinutos(LocalTime inicio,LocalTime fim){
		long m=Duration.between(inicio,fim).toMinutes();
		return (int)(m<0?m+24*60:m);
	}

	private static BigDecimal casas(BigDecimal v){return v.setScale(2,RoundingMode.HALF_UP);}
}
