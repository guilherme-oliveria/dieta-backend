package com.br.dietamais.dietas.dominio;

import com.br.dietamais.alimentos.api.AlimentoParaDieta;
import com.br.dietamais.dietas.api.TotaisNutricionaisResposta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/** RN-DIE-05: totais calculados no backend a partir das porções; cada item arredonda a 2 casas (HALF_UP) e as somas usam os itens arredondados. */
public final class CalculadoraTotais {
	private CalculadoraTotais(){}
	public static final TotaisNutricionaisResposta ZERO=new TotaisNutricionaisResposta(escala(BigDecimal.ZERO),escala(BigDecimal.ZERO),escala(BigDecimal.ZERO),escala(BigDecimal.ZERO));
	public static TotaisNutricionaisResposta item(AlimentoParaDieta alimento,BigDecimal porcoes){
		return new TotaisNutricionaisResposta(escala(alimento.kcalPorPorcao().multiply(porcoes)),escala(alimento.carboidratosPorPorcao().multiply(porcoes)),
			escala(alimento.proteinasPorPorcao().multiply(porcoes)),escala(alimento.gordurasPorPorcao().multiply(porcoes)));
	}
	public static BigDecimal gramas(AlimentoParaDieta alimento,BigDecimal porcoes){return escala(alimento.gramasPorPorcao().multiply(porcoes));}
	public static TotaisNutricionaisResposta somar(Collection<TotaisNutricionaisResposta> partes){
		var kcal=BigDecimal.ZERO;var c=BigDecimal.ZERO;var p=BigDecimal.ZERO;var g=BigDecimal.ZERO;
		for(var t:partes){kcal=kcal.add(t.kcal());c=c.add(t.carboidratos());p=p.add(t.proteinas());g=g.add(t.gorduras());}
		return new TotaisNutricionaisResposta(escala(kcal),escala(c),escala(p),escala(g));
	}
	private static BigDecimal escala(BigDecimal valor){return valor.setScale(2,RoundingMode.HALF_UP);}
}
