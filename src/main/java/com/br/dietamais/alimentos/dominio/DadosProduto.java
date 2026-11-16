package com.br.dietamais.alimentos.dominio;

import com.br.dietamais.comum.api.*;
import java.math.BigDecimal;
import java.util.*;

/** Produto já validado e normalizado (RN-ALI-05). Texto em branco vira nulo; nutrientes só com chaves conhecidas, valores de 0 a menos de 10^10 e até 4 casas. */
public record DadosProduto(String nome,String marca,String classificacao,String apresentacao,String embalagem,String sabores,String medidaCaseira,
		BigDecimal medidaGramas,String observacao,Map<String,BigDecimal> nutrientes,boolean ativo){
	private static final BigDecimal LIMITE=new BigDecimal("10000000000");
	private static String texto(String s,int max){
		if(s==null||s.isBlank())return null;
		var t=s.strip();if(t.length()>max)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-ALI-05");return t;
	}
	public DadosProduto {
		nome=texto(nome,150);marca=texto(marca,80);classificacao=texto(classificacao,80);apresentacao=texto(apresentacao,40);embalagem=texto(embalagem,200);
		sabores=texto(sabores,400);medidaCaseira=texto(medidaCaseira,60);observacao=texto(observacao,1200);
		if(nome==null||medidaGramas!=null&&(medidaGramas.signum()<=0||medidaGramas.stripTrailingZeros().scale()>4||medidaGramas.compareTo(new BigDecimal("100000000"))>=0)
			||nutrientes==null||nutrientes.size()>NutrientesProduto.CHAVES.size())throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-ALI-05");
		var limpo=new TreeMap<String,BigDecimal>();
		for(var e:nutrientes.entrySet()){
			var v=e.getValue();
			if(!NutrientesProduto.CHAVES.contains(e.getKey())||v==null||v.signum()<0||v.stripTrailingZeros().scale()>4||v.compareTo(LIMITE)>=0)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-ALI-05");
			limpo.put(e.getKey(),v.stripTrailingZeros().scale()<0?v.setScale(0):v.stripTrailingZeros());
		}
		if(!limpo.keySet().containsAll(NutrientesProduto.MINIMOS))throw new ExcecaoNegocio(CodigoErro.OBRIGATORIO,"RN-ALI-05");
		nutrientes=Collections.unmodifiableMap(limpo);
	}
}
