package com.br.dietamais.dietas.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Escala das porções de um modelo para o alvo de energia do paciente (RN-MOD-02): arredonda para 0,5 e nunca abaixo de 0,5. */
public final class EscalaPorcoes {
	public static final BigDecimal MIN=new BigDecimal("0.25"),MAX=new BigDecimal("4"),MEIA=new BigDecimal("0.5");
	private EscalaPorcoes(){}
	/** Alvo dividido pela base, com 4 casas; fora de {@code [0,25; 4]} é inviável e devolve {@code null}. */
	public static BigDecimal fator(BigDecimal alvoKcal,BigDecimal baseKcal){
		if(alvoKcal==null||baseKcal==null||baseKcal.signum()<=0||alvoKcal.signum()<=0)return null;
		var f=alvoKcal.divide(baseKcal,4,RoundingMode.HALF_UP);
		return f.compareTo(MIN)<0||f.compareTo(MAX)>0?null:f;
	}
	public static BigDecimal escalar(BigDecimal porcoes,BigDecimal fator){
		if(porcoes==null)return null;
		var v=porcoes.multiply(fator).divide(MEIA,0,RoundingMode.HALF_UP).multiply(MEIA);
		return v.compareTo(MEIA)<0?MEIA:v;
	}
}
