package com.br.dietamais.dietas.dominio;

import com.br.dietamais.alimentos.api.AlimentoParaDieta;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.referencias.api.PorcoesGrupos;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * RN-DIE-05: totais calculados no backend a partir das porções; cada item por alimento arredonda a 2 casas (HALF_UP) e as somas usam os itens arredondados.
 * RN-CAL-07: no item por grupo a soma usa o perfil da porção sem arredondar e só o total (da refeição ou do dia) arredonda a 2 casas.
 */
public final class CalculadoraTotais {
	private CalculadoraTotais(){}
	private static final BigDecimal CEM=new BigDecimal("100"),QUATRO=new BigDecimal("4"),NOVE=new BigDecimal("9");
	public static final TotaisNutricionaisResposta ZERO=new TotaisNutricionaisResposta(escala(BigDecimal.ZERO),escala(BigDecimal.ZERO),escala(BigDecimal.ZERO),escala(BigDecimal.ZERO));
	/** Macros sem arredondar, para somar antes de arredondar o total. */
	public record Parcela(BigDecimal kcal,BigDecimal carboidratos,BigDecimal proteinas,BigDecimal gorduras) {
		public static final Parcela ZERO=new Parcela(BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);
		public Parcela mais(Parcela o){return new Parcela(kcal.add(o.kcal),carboidratos.add(o.carboidratos),proteinas.add(o.proteinas),gorduras.add(o.gorduras));}
		public Parcela menos(Parcela o){return new Parcela(kcal.subtract(o.kcal),carboidratos.subtract(o.carboidratos),proteinas.subtract(o.proteinas),gorduras.subtract(o.gorduras));}
		public TotaisNutricionaisResposta totais(){return new TotaisNutricionaisResposta(escala(kcal),escala(carboidratos),escala(proteinas),escala(gorduras));}
		static Parcela de(TotaisNutricionaisResposta t){return new Parcela(t.kcal(),t.carboidratos(),t.proteinas(),t.gorduras());}
	}
	public static TotaisNutricionaisResposta item(AlimentoParaDieta alimento,BigDecimal porcoes){
		return new TotaisNutricionaisResposta(escala(alimento.kcalPorPorcao().multiply(porcoes)),escala(alimento.carboidratosPorPorcao().multiply(porcoes)),
			escala(alimento.proteinasPorPorcao().multiply(porcoes)),escala(alimento.gordurasPorPorcao().multiply(porcoes)));
	}
	/** Item por grupo: porções × perfil da opção escolhida, sem arredondar (RN-CAL-07). Nutriente que o perfil não informa conta como zero nos 4 macros (os vetores cobrem todos). */
	public static Parcela porGrupo(PorcoesGrupos.Opcao opcao,BigDecimal porcoes){
		var n=opcao.nutrientes();
		return new Parcela(n.getOrDefault("energia",opcao.kcal()).multiply(porcoes),n.getOrDefault("gli",BigDecimal.ZERO).multiply(porcoes),
			n.getOrDefault("pro",BigDecimal.ZERO).multiply(porcoes),n.getOrDefault("lip",BigDecimal.ZERO).multiply(porcoes));
	}
	public static Parcela exata(TotaisNutricionaisResposta itemArredondado){return Parcela.de(itemArredondado);}
	public static Parcela somarExato(Collection<Parcela> partes){
		var t=Parcela.ZERO;for(var p:partes)t=t.mais(p);return t;
	}
	public static BigDecimal gramas(AlimentoParaDieta alimento,BigDecimal porcoes){return escala(alimento.gramasPorPorcao().multiply(porcoes));}
	public static TotaisNutricionaisResposta somar(Collection<TotaisNutricionaisResposta> partes){
		var kcal=BigDecimal.ZERO;var c=BigDecimal.ZERO;var p=BigDecimal.ZERO;var g=BigDecimal.ZERO;
		for(var t:partes){kcal=kcal.add(t.kcal());c=c.add(t.carboidratos());p=p.add(t.proteinas());g=g.add(t.gorduras());}
		return new TotaisNutricionaisResposta(escala(kcal),escala(c),escala(p),escala(g));
	}
	/** Meta menos total do dia: VET em kcal; carboidrato e proteína a 4 kcal/g, lipídio a 9 kcal/g (RN-CAL-07). */
	public static SaldoMetasResposta saldo(MetasDieta m,TotaisNutricionaisResposta dia){
		return new SaldoMetasResposta(escala(m.vetKcal().subtract(dia.kcal())),
			escala(gramasMeta(m,m.carboidratoPct(),QUATRO).subtract(dia.carboidratos())),
			escala(gramasMeta(m,m.proteinaPct(),QUATRO).subtract(dia.proteinas())),
			escala(gramasMeta(m,m.lipidioPct(),NOVE).subtract(dia.gorduras())));
	}
	private static BigDecimal gramasMeta(MetasDieta m,BigDecimal pct,BigDecimal kcalPorGrama){
		return m.vetKcal().multiply(pct).divide(CEM.multiply(kcalPorGrama),6,RoundingMode.HALF_UP);
	}
	/** Total da alternativa menos o da refeição que ela substitui. */
	public static DiferencaTotaisResposta diferenca(TotaisNutricionaisResposta alternativa,TotaisNutricionaisResposta principal){
		return new DiferencaTotaisResposta(alternativa.kcal().subtract(principal.kcal()),alternativa.carboidratos().subtract(principal.carboidratos()),
			alternativa.proteinas().subtract(principal.proteinas()),alternativa.gorduras().subtract(principal.gorduras()));
	}
	private static BigDecimal escala(BigDecimal valor){return valor.setScale(2,RoundingMode.HALF_UP);}
}
