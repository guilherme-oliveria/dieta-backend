package com.br.dietamais.avaliacoes.dominio.energia;

import com.br.dietamais.avaliacoes.api.NivelAtividade;
import java.math.*;
import java.util.*;

/**
 * GET pelo equivalente metabólico (aba MET da planilha, plano das planilhas seção 3.2), indicado para quem treina com regularidade.
 * {@code GEB/h = Cunningham/24}; sono {@code GEB/h × horas × 0,9}; trabalho {@code GEB/h × horas × fator}; até 5 atividades
 * {@code minutos/60 × MET × peso}; rotina {@code GEB/h × horas restantes × 1,3}; GET = soma. Kcal com 2 casas em HALF_UP, intermediários sem arredondar.
 */
public final class CalculadoraGetPorMet {
	public static final int MAXIMO_ATIVIDADES=5;
	private static final MathContext MC=new MathContext(24,RoundingMode.HALF_UP);
	private static final BigDecimal SESSENTA=BigDecimal.valueOf(60),VINTE_QUATRO=BigDecimal.valueOf(24);
	private CalculadoraGetPorMet(){}

	/** Uma atividade da rotina: o MET vem de {@code referencia_met}. */
	public record AtividadeDia(BigDecimal met,BigDecimal minutos){}
	public record Entrada(String sexo,BigDecimal pesoKg,BigDecimal massaMagraKg,BigDecimal horasSono,BigDecimal horasTrabalho,NivelAtividade intensidadeTrabalho,List<AtividadeDia> atividades){}
	public record Resultado(BigDecimal gebDia,BigDecimal gebHora,BigDecimal gastoSono,BigDecimal gastoTrabalho,List<BigDecimal> gastoAtividades,BigDecimal horasRotina,BigDecimal gastoRotina,BigDecimal get){}

	public static Resultado calcular(Entrada e){
		if(e.atividades().size()>MAXIMO_ATIVIDADES)throw new IllegalArgumentException("No máximo "+MAXIMO_ATIVIDADES+" atividades");
		var gebDia=CalculadoraEnergia.gebCunningham(e.massaMagraKg());
		var gebHora=gebDia.divide(VINTE_QUATRO,MC);
		var sono=gebHora.multiply(e.horasSono()).multiply(new BigDecimal("0.9"));
		var trabalho=gebHora.multiply(e.horasTrabalho()).multiply(fatorTrabalho(e.sexo(),e.intensidadeTrabalho()));
		var minutosAtividades=BigDecimal.ZERO;var gastos=new ArrayList<BigDecimal>();var total=sono.add(trabalho);
		for(var a:e.atividades()){
			var g=a.minutos().divide(SESSENTA,MC).multiply(a.met()).multiply(e.pesoKg());
			gastos.add(g.setScale(2,RoundingMode.HALF_UP));total=total.add(g);minutosAtividades=minutosAtividades.add(a.minutos());
		}
		var horasRotina=VINTE_QUATRO.subtract(e.horasSono()).subtract(e.horasTrabalho()).subtract(minutosAtividades.divide(SESSENTA,MC));
		if(horasRotina.signum()<0)throw new IllegalArgumentException("As horas do dia passam de 24");
		var rotina=gebHora.multiply(horasRotina).multiply(new BigDecimal("1.3"));
		total=total.add(rotina);
		return new Resultado(gebDia,esc(gebHora),esc(sono),esc(trabalho),gastos,esc(horasRotina),esc(rotina),esc(total));
	}
	/** Fatores da planilha: leve 1,55 (H) / 1,56 (M); moderada 1,78 / 1,64; intensa 2,1 / 1,82. Sedentário não existe na aba MET. */
	static BigDecimal fatorTrabalho(String sexo,NivelAtividade nivel){
		boolean h="masculino".equals(sexo);
		if(!h&&!"feminino".equals(sexo))throw new IllegalArgumentException("Sexo desconhecido: "+sexo);
		return new BigDecimal(switch(nivel){
			case LEVE->h?"1.55":"1.56";
			case MODERADO->h?"1.78":"1.64";
			case INTENSO->h?"2.1":"1.82";
			case SEDENTARIO->throw new IllegalArgumentException("Intensidade do trabalho: leve, moderada ou intensa");
		});
	}
	private static BigDecimal esc(BigDecimal v){return v.setScale(2,RoundingMode.HALF_UP);}
}
