package com.br.dietamais.avaliacoes.dominio.infantil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Localiza a medida entre cortes publicados, sem interpolar percentis ou escores Z. */
public final class CortesCrescimento {
	private CortesCrescimento(){}

	/** Limites exclusivos, exceto quando ambos coincidem. Nulo representa extremo aberto. */
	public record Intervalo(BigDecimal inferior,BigDecimal superior){
		public boolean exato(){return inferior!=null&&superior!=null&&inferior.compareTo(superior)==0;}
	}

	/** Fonte incompleta ou não estritamente crescente não permite determinar uma posição segura. */
	public static Optional<Intervalo> localizar(BigDecimal medida,List<BigDecimal> cortes,List<BigDecimal> posicoes){
		if(medida==null||cortes==null||posicoes==null||cortes.isEmpty()||cortes.size()!=posicoes.size())return Optional.empty();
		for(int i=0;i<cortes.size();i++){
			if(cortes.get(i)==null||posicoes.get(i)==null)return Optional.empty();
			if(i>0&&(cortes.get(i-1).compareTo(cortes.get(i))>=0||posicoes.get(i-1).compareTo(posicoes.get(i))>=0))return Optional.empty();
		}
		for(int i=0;i<cortes.size();i++){
			int comparacao=medida.compareTo(cortes.get(i));
			if(comparacao==0)return Optional.of(new Intervalo(posicoes.get(i),posicoes.get(i)));
			if(comparacao<0)return Optional.of(new Intervalo(i==0?null:posicoes.get(i-1),posicoes.get(i)));
		}
		return Optional.of(new Intervalo(posicoes.getLast(),null));
	}
}
