package com.br.dietamais.avaliacoes.aplicacao;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.dominio.infantil.CalculadoraInfantil;
import com.br.dietamais.avaliacoes.dominio.infantil.CortesCrescimento;
import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/** Aplica as curvas disponíveis sem inventar um percentil/Z entre os cortes da planilha. */
@Component
public class CalculoCurvasInfantis {
	private static final String FONTE="OMS 2006/2007 (planilhas da Nut. Luciana: abas P.I., E.I. e I.M.C.)";
	private static final String VERSAO="oms-1.0.0";
	private static final List<BigDecimal> P=CurvaOms.PERCENTIS.stream().map(s->new BigDecimal(s.replace(',','.'))).toList();
	private static final List<BigDecimal> Z=CurvaOms.ESCORES_Z.stream().map(BigDecimal::new).toList();
	private final ConsultaOms oms;
	public CalculoCurvasInfantis(ConsultaOms oms){this.oms=oms;}

	/** Estatura chega em metros; a curva de estatura é expressa em centímetros. */
	public CurvaCrescimentoAvaliacao calcular(String indicador,String sexo,int idadeMeses,BigDecimal medida,ModoCurva modo){
		if(!CurvaOms.INDICADORES.contains(indicador))throw new IllegalArgumentException("Indicador OMS desconhecido");
		modo=modo==null?ModoCurva.PERCENTIL:modo;
		boolean estatura=indicador.equals("ESTATURA_IDADE");
		String unidade=estatura?"cm":indicador.equals("PESO_IDADE")?"kg":"kg/m²";
		BigDecimal valor=medida==null?null:estatura?medida.multiply(new BigDecimal("100")):medida;
		if(idadeMeses<0||idadeMeses>=216)return semDados(indicador,valor,unidade,modo,"IDADE_FORA_DA_REFERENCIA");
		if(!"masculino".equals(sexo)&&!"feminino".equals(sexo))return semDados(indicador,valor,unidade,modo,"FALTA_SEXO");
		if(valor==null)return semDados(indicador,null,unidade,modo,"FALTA_MEDIDA");
		var curva=oms.curva(indicador,sexo,idadeMeses).orElse(null);
		if(curva==null)return semDados(indicador,valor,unidade,modo,"REFERENCIA_INDISPONIVEL");
		var percentil=CortesCrescimento.localizar(valor,curva.percentis(),P).orElse(null);
		var z=CortesCrescimento.localizar(valor,curva.escoresZ(),Z).orElse(null);
		boolean valida=percentil!=null&&z!=null&&curva.percentis().size()==P.size()&&curva.escoresZ().size()==Z.size();
		String classe=valida?classificar(indicador,idadeMeses,valor,modo,curva):null;
		return new CurvaCrescimentoAvaliacao(indicador,valor,unidade,curva.idadeMeses(),modo,classe,
			valida?intervalo(percentil):null,valida?intervalo(z):null,curva.percentis(),curva.escoresZ(),FONTE,VERSAO,valida?null:"REFERENCIA_INCONSISTENTE");
	}
	private static IntervaloCurva intervalo(CortesCrescimento.Intervalo i){return new IntervaloCurva(i.inferior(),i.superior());}
	private static CurvaCrescimentoAvaliacao semDados(String indicador,BigDecimal valor,String unidade,ModoCurva modo,String motivo){
		return new CurvaCrescimentoAvaliacao(indicador,valor,unidade,null,modo,null,null,null,null,null,FONTE,VERSAO,motivo);
	}
	private static String classificar(String indicador,int meses,BigDecimal valor,ModoCurva modo,CurvaOms curva){
		boolean p=modo==ModoCurva.PERCENTIL;var c=p?curva.percentis():curva.escoresZ();
		if(!indicador.equals("IMC_IDADE"))return CalculadoraInfantil.classificarEstaturaIdade(valor,c.get(0),c.get(1),c.get(p?6:5)).name();
		if(meses>=120)return CalculadoraInfantil.classificarImcAdolescente(valor,c.get(1),c.get(p?5:4),c.get(p?6:5)).name();
		return CalculadoraInfantil.classificarImcCrianca(valor,c.get(0),c.get(1),c.get(p?5:4),c.get(p?6:5),c.get(p?7:6)).name();
	}
}
