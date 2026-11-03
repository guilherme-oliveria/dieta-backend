package com.br.dietamais.alimentos.dominio;

import com.br.dietamais.comum.api.*;
import java.math.BigDecimal;
import java.util.UUID;

public record DadosAlimento(UUID grupoId,String nome,String medidaCaseira,BigDecimal gramas,boolean ativo){
	public DadosAlimento {
		nome=nome==null?null:nome.strip();medidaCaseira=medidaCaseira==null?null:medidaCaseira.strip();
		if(grupoId==null||nome==null||nome.isBlank()||nome.length()>150||medidaCaseira==null||medidaCaseira.isBlank()||medidaCaseira.length()>150
			||gramas==null||gramas.signum()<=0||gramas.stripTrailingZeros().scale()>4||gramas.compareTo(new BigDecimal("100000000"))>=0)
			throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-ALI-01");
	}
}
