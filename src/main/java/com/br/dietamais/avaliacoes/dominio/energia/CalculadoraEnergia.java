package com.br.dietamais.avaliacoes.dominio.energia;

import com.br.dietamais.avaliacoes.api.*;
import java.math.*;

/**
 * Porta da aba "macronutrientes e calorias" da planilha da Nut. Luciana (plano das planilhas, seção 3.2) com as correções
 * da seção 3.6 (item 6: a proteína mínima do protocolo 2 usa a idade). Funções puras com {@link BigDecimal}: kcal, g e %
 * guardados com 2 casas em HALF_UP, intermediários sem arredondar. RN-CAL-01. Adulto e idoso (18+); abaixo disso o
 * cálculo é do BE-55.
 */
public final class CalculadoraEnergia {
	private static final MathContext MC=new MathContext(24,RoundingMode.HALF_UP);
	private static final BigDecimal CEM=BigDecimal.valueOf(100),QUATRO=BigDecimal.valueOf(4),NOVE=BigDecimal.valueOf(9);
	private CalculadoraEnergia(){}

	// ---------- GEB (kcal/dia) ----------
	/** FAO/OMS 1985. */
	public static BigDecimal gebFao1985(String sexo,int idadeAnos,BigDecimal pesoKg){
		boolean h=masculino(sexo);int f=faixa(idadeAnos);
		double[][] hm={{15.3,679},{11.6,879},{13.5,487}},mu={{14.7,496},{8.7,829},{10.5,596}};
		var c=(h?hm:mu)[f];
		return kcal(pesoKg.multiply(d(c[0])).add(d(c[1])));
	}
	/** FAO/OMS 2004. */
	public static BigDecimal gebFao2004(String sexo,int idadeAnos,BigDecimal pesoKg){
		boolean h=masculino(sexo);int f=faixa(idadeAnos);
		double[][] hm={{15.057,692.2},{11.472,873.1},{11.711,587.7}},mu={{14.818,486.6},{8.126,845.6},{9.082,658.5}};
		var c=(h?hm:mu)[f];
		return kcal(pesoKg.multiply(d(c[0])).add(d(c[1])));
	}
	/** IOM 2002; {@code sobrepesoOuObeso} escolhe o par de fórmulas (eutrófico/magro × sobrepeso/obeso). */
	public static BigDecimal gebIom2002(String sexo,int idadeAnos,BigDecimal pesoKg,BigDecimal estaturaM,boolean sobrepesoOuObeso){
		var i=BigDecimal.valueOf(idadeAnos);
		if(masculino(sexo)){
			return sobrepesoOuObeso?kcal(d(293).subtract(i.multiply(d(3.8))).add(estaturaM.multiply(d(456.4))).add(pesoKg.multiply(d(10.12))))
				:kcal(d(204).subtract(i.multiply(d(4))).add(estaturaM.multiply(d(450.5))).add(pesoKg.multiply(d(11.69))));
		}
		return sobrepesoOuObeso?kcal(d(247).subtract(i.multiply(d(2.67))).add(estaturaM.multiply(d(401.5))).add(pesoKg.multiply(d(8.6))))
			:kcal(d(255).subtract(i.multiply(d(2.35))).add(estaturaM.multiply(d(361.6))).add(pesoKg.multiply(d(9.39))));
	}
	/** Cunningham 1991: {@code massaMagraKg} é a massa magra (ou músculo + osso + residual no protocolo 2). */
	public static BigDecimal gebCunningham(BigDecimal massaMagraKg){return kcal(d(370).add(massaMagraKg.multiply(d(21.6))));}

	// ---------- GET (kcal/dia) ----------
	public static BigDecimal getFao1985(BigDecimal geb,String sexo,NivelAtividade nivel){
		boolean h=masculino(sexo);
		double fator=switch(nivel){case SEDENTARIO->1.3;case LEVE->h?1.55:1.56;case MODERADO->h?1.78:1.64;case INTENSO->h?2.1:1.82;};
		return kcal(geb.multiply(d(fator)));
	}
	/** FAO 2004: mesmos fatores para os dois sexos; lactante soma 675 kcal. */
	public static BigDecimal getFao2004(BigDecimal geb,NivelAtividade nivel,boolean lactante){
		double fator=switch(nivel){case SEDENTARIO,LEVE->1.53;case MODERADO->1.76;case INTENSO->2.25;};
		var get=geb.multiply(d(fator));
		return kcal(lactante?get.add(d(675)):get);
	}
	/** IOM 2005: GEB do IOM vezes 1,2 / 1,5 / 1,75 / 2,2. */
	public static BigDecimal getIom2005(BigDecimal gebIom,NivelAtividade nivel){
		double fator=switch(nivel){case SEDENTARIO->1.2;case LEVE->1.5;case MODERADO->1.75;case INTENSO->2.2;};
		return kcal(gebIom.multiply(d(fator)));
	}
	/** RDI 2005 (EER); {@code lactacao} nulo = não lactante. */
	public static BigDecimal getRdi2005(String sexo,int idadeAnos,BigDecimal pesoKg,BigDecimal estaturaM,NivelAtividade nivel,PeriodoLactacao lactacao){
		var i=BigDecimal.valueOf(idadeAnos);BigDecimal eer;
		if(masculino(sexo)){
			double af=switch(nivel){case SEDENTARIO->1;case LEVE->1.11;case MODERADO->1.25;case INTENSO->1.48;};
			eer=d(662).subtract(i.multiply(d(9.53))).add(d(af).multiply(pesoKg.multiply(d(15.91)).add(estaturaM.multiply(d(539.36)))));
		}else{
			double af=switch(nivel){case SEDENTARIO->1;case LEVE->1.12;case MODERADO->1.27;case INTENSO->1.45;};
			eer=d(354).subtract(i.multiply(d(6.91))).add(d(af).multiply(pesoKg.multiply(d(9.36)).add(estaturaM.multiply(d(727)))));
			if(lactacao!=null)eer=eer.add(d(lactacao==PeriodoLactacao.ATE_6_MESES?330:400));
		}
		return kcal(eer);
	}

	// ---------- VET ----------
	/** Manutenção = GET; ganho soma e perda subtrai {@code (kg/mês ÷ 4 × 1000) × 500/450} kcal por dia (a planilha, tal como está). */
	public static BigDecimal vet(BigDecimal get,ObjetivoPeso objetivo,BigDecimal kgPorMes){
		if(objetivo==ObjetivoPeso.MANUTENCAO||kgPorMes==null)return kcal(get);
		var ajuste=kgPorMes.divide(QUATRO,MC).multiply(BigDecimal.valueOf(1000)).multiply(BigDecimal.valueOf(500)).divide(BigDecimal.valueOf(450),MC);
		return kcal(objetivo==ObjetivoPeso.GANHO?get.add(ajuste):get.subtract(ajuste));
	}
	public static BigDecimal kcalPorKg(BigDecimal vet,BigDecimal pesoKg){return kcal(vet.divide(pesoKg,MC));}
	/**
	 * RN-CAL-05: peso para os "por kg". Adulto (18+) com IMC de 30 ou mais usa o peso ajustado {@code (P − Pideal) × 0,25 + Pideal}, com
	 * {@code Pideal = E² × 21,7} (meio da faixa de IMC 18,5 a 24,9); os demais usam o peso atual. Resultado com 2 casas em HALF_UP.
	 */
	public static BigDecimal pesoParaKg(BigDecimal pesoKg,BigDecimal estaturaM,int idadeAnos){
		if(idadeAnos<18||estaturaM==null||estaturaM.signum()<=0)return pesoKg;
		var quadrado=estaturaM.multiply(estaturaM);
		if(pesoKg.divide(quadrado,MC).compareTo(BigDecimal.valueOf(30))<0)return pesoKg;
		var ideal=quadrado.multiply(new BigDecimal("21.7"));
		return kcal(pesoKg.subtract(ideal).multiply(new BigDecimal("0.25")).add(ideal));
	}

	// ---------- Metas de macronutrientes ----------
	public static MetaMacro carboidrato(ProtocoloCarboidrato protocolo,BigDecimal vet,BigDecimal pesoKg,boolean gestante,boolean lactante){
		return switch(protocolo){
			case FAIXA_DRI->porPercentual(vet,45,65,QUATRO);
			case FAIXA_ESPORTIVA->porPercentual(vet,50,60,QUATRO);
			case POR_KG->porKg(vet,pesoKg,d(5),d(10),QUATRO);
			case MINIMO_RDA->minimoEmGramas(vet,d(lactante?210:gestante?175:130),QUATRO);
		};
	}
	/** RDA (protocolo 2), da macro {@code RDI_PTN}: 71 g para gestante e lactante; adulto 56 g (homem) e 46 g (mulher). Adolescente e criança ficam no BE-55. */
	public static MetaMacro proteina(ProtocoloProteina protocolo,BigDecimal vet,BigDecimal pesoKg,String sexo,int idadeAnos,boolean gestante,boolean lactante){
		return switch(protocolo){
			case FAIXA_DRI->porPercentual(vet,10,35,QUATRO);
			case MINIMO_15->minimoPercentual(vet,15,QUATRO);
			case ENDURANCE->porKg(vet,pesoKg,d(1.2),d(1.6),QUATRO);
			case HIPERTROFIA->porKg(vet,pesoKg,d(1.4),d(1.8),QUATRO);
			case RDA->minimoEmGramas(vet,d(lactante||gestante?71:masculino(sexo)?56:46),QUATRO);
		};
	}
	/** Lipídio: mínimo 20% (protocolo 1) ou 25% (protocolo 2) e máximo 35%. */
	public static MetaMacro lipidio(ProtocoloLipidio protocolo,BigDecimal vet){return porPercentual(vet,protocolo==ProtocoloLipidio.MINIMO_20?20:25,35,NOVE);}
	/** Dislipidêmico (SBC 2007): saturado 7%, mono 20%, poli 10%, trans 1% do VET. */
	public static LipidiosDislipidemico lipidiosDislipidemico(BigDecimal vet){
		return new LipidiosDislipidemico(porPercentual(vet,20,35,NOVE),fixo(vet,7),fixo(vet,20),fixo(vet,10),fixo(vet,1));
	}
	/** 300 mg/dia, ou 200 se o colesterol total da bioquímica for 200 ou mais. */
	public static BigDecimal colesterolMaximoMg(BigDecimal colesterolTotal){return colesterolTotal!=null&&colesterolTotal.compareTo(d(200))>=0?d(200):d(300);}

	// ---------- auxiliares ----------
	private static MetaMacro porPercentual(BigDecimal vet,double min,double max,BigDecimal kcalPorG){
		var pMin=d(min);var pMax=d(max);
		return new MetaMacro(esc(pMin),esc(pMax),gramas(vet,pMin,kcalPorG),gramas(vet,pMax,kcalPorG));
	}
	private static MetaMacro minimoPercentual(BigDecimal vet,double min,BigDecimal kcalPorG){var p=d(min);return new MetaMacro(esc(p),null,gramas(vet,p,kcalPorG),null);}
	private static MetaMacro fixo(BigDecimal vet,double percentual){var p=d(percentual);return new MetaMacro(esc(p),esc(p),gramas(vet,p,NOVE),gramas(vet,p,NOVE));}
	private static MetaMacro porKg(BigDecimal vet,BigDecimal pesoKg,BigDecimal gMin,BigDecimal gMax,BigDecimal kcalPorG){
		var min=gMin.multiply(pesoKg);var max=gMax.multiply(pesoKg);
		return new MetaMacro(percentualDe(min,vet,kcalPorG),percentualDe(max,vet,kcalPorG),esc(min),esc(max));
	}
	private static MetaMacro minimoEmGramas(BigDecimal vet,BigDecimal gMin,BigDecimal kcalPorG){return new MetaMacro(percentualDe(gMin,vet,kcalPorG),null,esc(gMin),null);}
	private static BigDecimal gramas(BigDecimal vet,BigDecimal percentual,BigDecimal kcalPorG){return esc(percentual.multiply(vet).divide(CEM,MC).divide(kcalPorG,MC));}
	private static BigDecimal percentualDe(BigDecimal gramas,BigDecimal vet,BigDecimal kcalPorG){return esc(gramas.multiply(kcalPorG).multiply(CEM).divide(vet,MC));}
	private static boolean masculino(String sexo){
		if("masculino".equals(sexo))return true;
		if("feminino".equals(sexo))return false;
		throw new IllegalArgumentException("Sexo desconhecido: "+sexo);
	}
	private static int faixa(int idade){
		if(idade<18)throw new IllegalArgumentException("Fórmula de adulto: idade "+idade);
		return idade<30?0:idade<60?1:2;
	}
	private static BigDecimal d(double v){return BigDecimal.valueOf(v);}
	private static BigDecimal kcal(BigDecimal v){return esc(v);}
	private static BigDecimal esc(BigDecimal v){return v.setScale(2,RoundingMode.HALF_UP);}
}
