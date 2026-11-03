package com.br.dietamais.relatorios.aplicacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/** Escala apenas o número inicial: não altera dimensões ou números na descrição. */
final class EscalaMedidaCaseira {
	private static final BigDecimal QUATRO=BigDecimal.valueOf(4);
	private static final Pattern INICIO=Pattern.compile("^(?:(\\d+(?:[.,]\\d+)?)(?:\\s*([¼½¾]))?|([¼½¾]))(.*)$");
	private EscalaMedidaCaseira(){}
	static String escalar(String medida,BigDecimal fator){
		var m=INICIO.matcher(medida.strip());
		if(!m.matches())return formatar(fator)+" × "+medida;
		BigDecimal quantidade=m.group(1)==null?BigDecimal.ZERO:new BigDecimal(m.group(1).replace(',','.'));
		String fracao=m.group(2)!=null?m.group(2):m.group(3);
		if(fracao!=null)quantidade=quantidade.add(switch(fracao){case "¼"->new BigDecimal("0.25");case "½"->new BigDecimal("0.5");default->new BigDecimal("0.75");});
		return formatar(quantidade.multiply(fator))+(m.group(4).isBlank()?"":" "+m.group(4).strip());
	}
	private static String formatar(BigDecimal quantidade){
		var quartos=quantidade.multiply(QUATRO).setScale(0,RoundingMode.HALF_UP).toBigIntegerExact();
		if(quartos.signum()==0)return "0";
		var divisao=quartos.divideAndRemainder(java.math.BigInteger.valueOf(4));
		return (divisao[0].signum()==0?"":divisao[0].toString())+switch(divisao[1].intValue()){case 1->"¼";case 2->"½";case 3->"¾";default->"";};
	}
}
