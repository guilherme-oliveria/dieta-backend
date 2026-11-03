package com.br.dietamais.avaliacoes.dominio.infantil;

import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;

/**
 * Porta das abas de 0 a 9 e de 10 a 17 anos (plano das planilhas, seção 3.3). Funções puras, RN-CAL-01.
 * Dado ausente devolve {@code null} (RN-CAL-04), nunca zero. Valores com 2 casas em HALF_UP; intermediários sem arredondar.
 * Vetores em {@code contrato/calculos/infantil.json} (RN-CAL-06), iguais aos do mobile.
 * <p>Correções sobre a planilha: a faixa de adequação do CMB já não se sobrepõe (90–110 A, acima de 110 até 120 EP);
 * a classificação da AMBc usa o P90 (a macro citava uma variável indefinida); o Slaughter de menino negro de 10 a 11 anos
 * usa a faixa certa (a macro comparava {@code idade > 12}); idade ou etnia fora da faixa devolve {@code null}.
 */
public final class CalculadoraInfantil {
	private static final MathContext MC=new MathContext(24,RoundingMode.HALF_UP);
	private static final BigDecimal PI_PLANILHA=new BigDecimal("3.14");
	private CalculadoraInfantil(){}

	public enum ClasseAdequacao{DG,DM,DL,A,EP,O}
	public enum ClassePercentilBraco{ABAIXO_P5,P5_A_P10,P10_A_P90,ACIMA_P90}
	public enum ClasseEstaturaIdade{MB,B,A,AC}
	public enum ClasseImcCrianca{MA,M,A,S,O,OG}
	public enum ClasseImcAdolescente{BP,NO,SP,OB}
	public enum ClasseGordura{MT_BXO,BXO,ADEQ,MOD_ALTO,ALTO,MT_ALTO}
	public enum ClassePesoNascer{EXTREMAMENTE_BAIXO,MUITO_BAIXO,BAIXO,INSUFICIENTE,NORMAL,MACROSSOMIA}

	/** Idade como "1 ano e 3 meses" (anos e meses completos na data). */
	public static String idadeTexto(LocalDate nascimento,LocalDate data){
		if(nascimento==null||data==null||data.isBefore(nascimento))return null;
		long meses=ChronoUnit.MONTHS.between(nascimento,data);
		long a=meses/12,m=meses%12;
		if(a==0&&m==0)return "menos de 1 mês";
		var sb=new StringBuilder();
		if(a>0)sb.append(a).append(a==1?" ano":" anos");
		if(m>0){if(a>0)sb.append(" e ");sb.append(m).append(m==1?" mês":" meses");}
		return sb.toString();
	}

	/** Peso ao nascer em gramas: &lt;1000, ≤1500, ≤2500, ≤3000, ≤4000, acima. */
	public static ClassePesoNascer classificarPesoNascer(Integer gramas){
		if(gramas==null||gramas<=0)return null;
		if(gramas<1000)return ClassePesoNascer.EXTREMAMENTE_BAIXO;
		if(gramas<=1500)return ClassePesoNascer.MUITO_BAIXO;
		if(gramas<=2500)return ClassePesoNascer.BAIXO;
		if(gramas<=3000)return ClassePesoNascer.INSUFICIENTE;
		if(gramas<=4000)return ClassePesoNascer.NORMAL;
		return ClassePesoNascer.MACROSSOMIA;
	}

	/** Faixa de peso esperada para um IMC de referência: {@code IMC × estatura²}. */
	public static BigDecimal pesoParaImc(BigDecimal imcReferencia,BigDecimal estaturaM){
		if(imcReferencia==null||estaturaM==null)return null;
		return esc(imcReferencia.multiply(estaturaM,MC).multiply(estaturaM,MC));
	}

	// ---------- Classificação pelas curvas (cortes vêm de /referencias/oms; aqui só as faixas) ----------
	/** Estatura/idade com cortes {@code a<b<c} (0,1/3/97 ou −3/−2/+2): abaixo de a MB; até b B; até c A; acima AC. */
	public static ClasseEstaturaIdade classificarEstaturaIdade(BigDecimal valor,BigDecimal a,BigDecimal b,BigDecimal c){
		if(valor==null||a==null||b==null||c==null)return null;
		if(valor.compareTo(a)<0)return ClasseEstaturaIdade.MB;
		if(valor.compareTo(b)<0)return ClasseEstaturaIdade.B;
		if(valor.compareTo(c)<=0)return ClasseEstaturaIdade.A;
		return ClasseEstaturaIdade.AC;
	}
	/** Peso/idade usa as mesmas faixas da estatura/idade. */
	public static ClasseEstaturaIdade classificarPesoIdade(BigDecimal valor,BigDecimal a,BigDecimal b,BigDecimal c){
		return classificarEstaturaIdade(valor,a,b,c);
	}
	/** IMC/idade (0–9) com cinco cortes: &lt;a MA; &lt;b M; &lt;c A; &lt;d S; ≤e O; &gt;e OG. */
	public static ClasseImcCrianca classificarImcCrianca(BigDecimal imc,BigDecimal a,BigDecimal b,BigDecimal c,BigDecimal d,BigDecimal e){
		if(imc==null||a==null||b==null||c==null||d==null||e==null)return null;
		if(imc.compareTo(a)<0)return ClasseImcCrianca.MA;
		if(imc.compareTo(b)<0)return ClasseImcCrianca.M;
		if(imc.compareTo(c)<0)return ClasseImcCrianca.A;
		if(imc.compareTo(d)<0)return ClasseImcCrianca.S;
		if(imc.compareTo(e)<=0)return ClasseImcCrianca.O;
		return ClasseImcCrianca.OG;
	}
	/** IMC/idade do adolescente com P3/P85/P97 (ou −2/+1/+2): &lt;a BP; &lt;b NO; &lt;c SP; senão OB. */
	public static ClasseImcAdolescente classificarImcAdolescente(BigDecimal imc,BigDecimal a,BigDecimal b,BigDecimal c){
		if(imc==null||a==null||b==null||c==null)return null;
		if(imc.compareTo(a)<0)return ClasseImcAdolescente.BP;
		if(imc.compareTo(b)<0)return ClasseImcAdolescente.NO;
		if(imc.compareTo(c)<0)return ClasseImcAdolescente.SP;
		return ClasseImcAdolescente.OB;
	}

	// ---------- Braço ----------
	/** Adequação em %: {@code medida / P50 × 100} (CMB, DCT, DCSE). */
	public static BigDecimal adequacao(BigDecimal medida,BigDecimal p50){
		if(medida==null||p50==null||p50.signum()<=0)return null;
		return esc(medida.multiply(BigDecimal.valueOf(100)).divide(p50,MC));
	}
	/** &lt;70 DG, 70–80 DM, &gt;80–90 DL, &gt;90–110 A, &gt;110–120 EP, &gt;120 O. */
	public static ClasseAdequacao classificarAdequacao(BigDecimal pct){
		if(pct==null)return null;
		if(pct.compareTo(bd(70))<0)return ClasseAdequacao.DG;
		if(pct.compareTo(bd(80))<=0)return ClasseAdequacao.DM;
		if(pct.compareTo(bd(90))<=0)return ClasseAdequacao.DL;
		if(pct.compareTo(bd(110))<=0)return ClasseAdequacao.A;
		if(pct.compareTo(bd(120))<=0)return ClasseAdequacao.EP;
		return ClasseAdequacao.O;
	}
	/** AMBc em mm²: {@code ((CB×10 − π·DCT)² / 4π) − 10} (menino) ou {@code − 6,5} (menina); CB em cm, DCT em mm, π = 3,14 como na planilha. */
	public static BigDecimal ambc(BigDecimal cbCm,BigDecimal dctMm,boolean masculino){
		if(cbCm==null||dctMm==null)return null;
		var t=cbCm.multiply(bd(10)).subtract(PI_PLANILHA.multiply(dctMm));
		var area=t.multiply(t).divide(PI_PLANILHA.multiply(bd(4)),MC);
		return esc(area.subtract(masculino?bd(10):new BigDecimal("6.5")));
	}
	/** AGB em mm²: área total do braço {@code π(CB×10)²/4π²} menos a AMBc. */
	public static BigDecimal agb(BigDecimal cbCm,BigDecimal dctMm,boolean masculino){
		var am=ambc(cbCm,dctMm,masculino);
		if(am==null)return null;
		var cb=cbCm.multiply(bd(10));
		var total=PI_PLANILHA.multiply(cb).multiply(cb).divide(PI_PLANILHA.multiply(PI_PLANILHA).multiply(bd(4)),MC);
		return esc(total.subtract(am));
	}
	/** Classe pelos percentis 5/10/90 de Frisancho (AMBc e AGB): &lt;P5; até P10; até P90; acima. */
	public static ClassePercentilBraco classificarPercentilBraco(BigDecimal valor,BigDecimal p5,BigDecimal p10,BigDecimal p90){
		if(valor==null||p5==null||p10==null||p90==null)return null;
		if(valor.compareTo(p5)<0)return ClassePercentilBraco.ABAIXO_P5;
		if(valor.compareTo(p10)<=0)return ClassePercentilBraco.P5_A_P10;
		if(valor.compareTo(p90)<=0)return ClassePercentilBraco.P10_A_P90;
		return ClassePercentilBraco.ACIMA_P90;
	}

	// ---------- Gordura do adolescente ----------
	/**
	 * Slaughter/Guedes: soma tricipital + subescapular em mm. Menina: {@code ≤35: 1,33S − 0,013S² − 2,5}; {@code >35: 0,546S + 9,7}.
	 * Menino: {@code ≤35: 1,21S − 0,008S² − k}, com k por etnia (1 = branca: 1,7 / 3,4 / 5,5; 2 = negra: 3,5 / 5,2 / 6,8) e faixa
	 * 10–11 / 12–13 / 14–17 anos; {@code >35: 0,783S + 1,6}. Menino ≤35 sem etnia ou fora de 10–17 anos: nulo.
	 */
	public static BigDecimal slaughter(boolean masculino,BigDecimal triciptalMm,BigDecimal subescapularMm,Integer idadeAnos,Integer etnia){
		if(triciptalMm==null||subescapularMm==null)return null;
		var s=triciptalMm.add(subescapularMm);
		var s2=s.multiply(s);
		if(!masculino){
			if(s.compareTo(bd(35))<=0)return esc(s.multiply(new BigDecimal("1.33")).subtract(s2.multiply(new BigDecimal("0.013"))).subtract(new BigDecimal("2.5")));
			return esc(s.multiply(new BigDecimal("0.546")).add(new BigDecimal("9.7")));
		}
		if(s.compareTo(bd(35))>0)return esc(s.multiply(new BigDecimal("0.783")).add(new BigDecimal("1.6")));
		if(idadeAnos==null||idadeAnos<10||idadeAnos>17||etnia==null||(etnia!=1&&etnia!=2))return null;
		int f=idadeAnos<12?0:idadeAnos<14?1:2;
		String[][] k={{"1.7","3.4","5.5"},{"3.5","5.2","6.8"}};
		return esc(s.multiply(new BigDecimal("1.21")).subtract(s2.multiply(new BigDecimal("0.008"))).subtract(new BigDecimal(k[etnia-1][f])));
	}
	/** Deurenberg 1990. Menino: &lt;6, ≤10, ≤20, ≤25, ≤31, acima; menina: &lt;12, ≤15, ≤25, ≤30, ≤36, acima. */
	public static ClasseGordura classificarGordura(boolean masculino,BigDecimal pct){
		if(pct==null)return null;
		int[] c=masculino?new int[]{6,10,20,25,31}:new int[]{12,15,25,30,36};
		if(pct.compareTo(bd(c[0]))<0)return ClasseGordura.MT_BXO;
		if(pct.compareTo(bd(c[1]))<=0)return ClasseGordura.BXO;
		if(pct.compareTo(bd(c[2]))<=0)return ClasseGordura.ADEQ;
		if(pct.compareTo(bd(c[3]))<=0)return ClasseGordura.MOD_ALTO;
		if(pct.compareTo(bd(c[4]))<=0)return ClasseGordura.ALTO;
		return ClasseGordura.MT_ALTO;
	}

	// ---------- Energia (IOM 2002/DRI) ----------
	/** GEB IOM: menino {@code 79 − 34,2I + 730E + 15,3P}; menina {@code 322 − 26I + 504E + 11,6P} (E em m, P em kg, I em anos). */
	public static BigDecimal gebIom(boolean masculino,BigDecimal idadeAnos,BigDecimal pesoKg,BigDecimal estaturaM){
		if(idadeAnos==null||pesoKg==null||estaturaM==null)return null;
		return masculino?esc(bd(79).subtract(idadeAnos.multiply(new BigDecimal("34.2"))).add(estaturaM.multiply(bd(730))).add(pesoKg.multiply(new BigDecimal("15.3"))))
			:esc(bd(322).subtract(idadeAnos.multiply(bd(26))).add(estaturaM.multiply(bd(504))).add(pesoKg.multiply(new BigDecimal("11.6"))));
	}
	/** GET IOM de 3 a 17 anos: menino {@code 114 − 50,9I + AF(19,5P + 1161E)}; menina {@code 389 − 41,2I + AF(15P + 701,6E)}. Menor de 3 anos: nulo. */
	public static BigDecimal getIom(boolean masculino,int idadeAnos,BigDecimal af,BigDecimal pesoKg,BigDecimal estaturaM){
		if(af==null||pesoKg==null||estaturaM==null||idadeAnos<3||idadeAnos>17)return null;
		var i=BigDecimal.valueOf(idadeAnos);
		return masculino?esc(bd(114).subtract(i.multiply(new BigDecimal("50.9"))).add(af.multiply(pesoKg.multiply(new BigDecimal("19.5")).add(estaturaM.multiply(bd(1161))))))
			:esc(bd(389).subtract(i.multiply(new BigDecimal("41.2"))).add(af.multiply(pesoKg.multiply(bd(15)).add(estaturaM.multiply(new BigDecimal("701.6"))))));
	}
	/**
	 * GET RDI (EER) até 17 anos. Menor de 1 ano: {@code 89P − 100} + 175 (até 3 meses), + 56 (4–6), + 22 (7–12); 1–2 anos: + 20;
	 * 3–8 anos: menino {@code 88,5 − 61,9I + AF(26,7P + 903E) + 20}, menina {@code 135,3 − 30,8I + AF(10P + 934E) + 20}; 9–17 anos: mesma fórmula com + 25.
	 * {@code meses} é o resto em meses dentro do ano (0–11) quando {@code anos}=0; AF e estatura só valem a partir dos 3 anos.
	 */
	public static BigDecimal getRdi(boolean masculino,int anos,int meses,BigDecimal af,BigDecimal pesoKg,BigDecimal estaturaM){
		if(pesoKg==null||anos<0||anos>17)return null;
		var base=pesoKg.multiply(bd(89)).subtract(bd(100));
		if(anos<1){
			int add=meses<4?175:meses<7?56:22;
			return esc(base.add(bd(add)));
		}
		if(anos<3)return esc(base.add(bd(20)));
		if(af==null||estaturaM==null)return null;
		var i=BigDecimal.valueOf(anos);int add=anos<9?20:25;
		var v=masculino?bd(88.5).subtract(i.multiply(new BigDecimal("61.9"))).add(af.multiply(pesoKg.multiply(new BigDecimal("26.7")).add(estaturaM.multiply(bd(903)))))
			:new BigDecimal("135.3").subtract(i.multiply(new BigDecimal("30.8"))).add(af.multiply(pesoKg.multiply(bd(10)).add(estaturaM.multiply(bd(934)))));
		return esc(v.add(bd(add)));
	}

	private static BigDecimal bd(double v){return BigDecimal.valueOf(v);}
	private static BigDecimal bd(int v){return BigDecimal.valueOf(v);}
	private static BigDecimal esc(BigDecimal v){return v.setScale(2,RoundingMode.HALF_UP);}
}
