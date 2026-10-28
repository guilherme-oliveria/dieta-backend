package com.br.dietamais.avaliacoes.dominio;

import com.br.dietamais.avaliacoes.api.ClassificacaoImc;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** RN-AVA-02: IMC = peso / estatura², 2 casas (HALF_UP), e classificação da OMS sobre o valor arredondado. */
public final class CalculadoraImc {
	private CalculadoraImc(){}
	public static BigDecimal imc(BigDecimal pesoKg,BigDecimal estaturaM){
		return pesoKg.divide(estaturaM.multiply(estaturaM),2,RoundingMode.HALF_UP);
	}
	public static ClassificacaoImc classificar(BigDecimal imc){
		if(imc.compareTo(new BigDecimal("18.5"))<0)return ClassificacaoImc.ABAIXO_DO_PESO;
		if(imc.compareTo(new BigDecimal("25"))<0)return ClassificacaoImc.NORMAL;
		if(imc.compareTo(new BigDecimal("30"))<0)return ClassificacaoImc.SOBREPESO;
		if(imc.compareTo(new BigDecimal("35"))<0)return ClassificacaoImc.OBESIDADE_I;
		if(imc.compareTo(new BigDecimal("40"))<0)return ClassificacaoImc.OBESIDADE_II;
		return ClassificacaoImc.OBESIDADE_III;
	}
}
