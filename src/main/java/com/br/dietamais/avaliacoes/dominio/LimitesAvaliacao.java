package com.br.dietamais.avaliacoes.dominio;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.comum.api.*;
import java.math.BigDecimal;
import java.util.List;

/** RN-AVA-03: limites de sanidade. Fora do intervalo = 422 {@code VALOR_FORA_DO_INTERVALO} com o campo; mais de 2 casas = 400. */
public final class LimitesAvaliacao {
	private LimitesAvaliacao(){}
	private static final BigDecimal PESO_MIN=new BigDecimal("20"),PESO_MAX=new BigDecimal("400"),EST_MIN=new BigDecimal("0.5"),EST_MAX=new BigDecimal("2.5");
	private static final BigDecimal PER_MIN=new BigDecimal("10"),PER_MAX=new BigDecimal("250"),DOB_MIN=BigDecimal.ONE,DOB_MAX=new BigDecimal("80"),PERC_MAX=new BigDecimal("100");

	public static void validar(SalvarAvaliacaoRequisicao r){
		faixa("pesoKg",r.pesoKg(),PESO_MIN,PESO_MAX);faixa("estaturaM",r.estaturaM(),EST_MIN,EST_MAX);
		var p=r.perimetros()==null?PerimetrosAvaliacao.VAZIO:r.perimetros();
		opcional("perimetros.pescoco",p.pescoco(),PER_MIN,PER_MAX);opcional("perimetros.torax",p.torax(),PER_MIN,PER_MAX);opcional("perimetros.cintura",p.cintura(),PER_MIN,PER_MAX);
		opcional("perimetros.abdomen",p.abdomen(),PER_MIN,PER_MAX);opcional("perimetros.quadril",p.quadril(),PER_MIN,PER_MAX);opcional("perimetros.braco",p.braco(),PER_MIN,PER_MAX);
		opcional("perimetros.antebraco",p.antebraco(),PER_MIN,PER_MAX);opcional("perimetros.coxa",p.coxa(),PER_MIN,PER_MAX);opcional("perimetros.panturrilha",p.panturrilha(),PER_MIN,PER_MAX);
		var d=r.dobras()==null?DobrasAvaliacao.VAZIO:r.dobras();
		opcional("dobras.triceps",d.triceps(),DOB_MIN,DOB_MAX);opcional("dobras.subescapular",d.subescapular(),DOB_MIN,DOB_MAX);opcional("dobras.peitoral",d.peitoral(),DOB_MIN,DOB_MAX);
		opcional("dobras.axilarMedia",d.axilarMedia(),DOB_MIN,DOB_MAX);opcional("dobras.suprailiaca",d.suprailiaca(),DOB_MIN,DOB_MAX);opcional("dobras.abdominal",d.abdominal(),DOB_MIN,DOB_MAX);
		opcional("dobras.coxa",d.coxa(),DOB_MIN,DOB_MAX);
		var c=r.composicao()==null?ComposicaoAvaliacao.VAZIO:r.composicao();
		opcional("composicao.percentualGordura",c.percentualGordura(),BigDecimal.ZERO,PERC_MAX);opcional("composicao.pesoGordoKg",c.pesoGordoKg(),BigDecimal.ZERO,PESO_MAX);
		opcional("composicao.pesoMagroKg",c.pesoMagroKg(),BigDecimal.ZERO,PESO_MAX);
		var m=r.complementares();
		if(m!=null){
			opcional("complementares.pesoUsualKg",m.pesoUsualKg(),PESO_MIN,PESO_MAX);opcional("complementares.pesoDesejadoKg",m.pesoDesejadoKg(),PESO_MIN,PESO_MAX);
			opcional("complementares.punhoCm",m.punhoCm(),new BigDecimal("5"),new BigDecimal("40"));
			opcional("complementares.diametroPunhoCm",m.diametroPunhoCm(),BigDecimal.ONE,new BigDecimal("30"));opcional("complementares.diametroJoelhoCm",m.diametroJoelhoCm(),BigDecimal.ONE,new BigDecimal("30"));
			opcional("complementares.percentualGorduraDesejado",m.percentualGorduraDesejado(),new BigDecimal("3"),new BigDecimal("60"));
			opcional("complementares.objetivoKgPorMes",m.objetivoKgPorMes(),BigDecimal.ZERO,BigDecimal.TEN);
		}
	}
	private static void opcional(String campo,BigDecimal v,BigDecimal min,BigDecimal max){if(v!=null)faixa(campo,v,min,max);}
	private static void faixa(String campo,BigDecimal v,BigDecimal min,BigDecimal max){
		if(v.stripTrailingZeros().scale()>2)throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null,"Use no máximo 2 casas decimais.",List.of(new ErroCampoResposta(campo,"FORMATO_INVALIDO","Use no máximo 2 casas decimais.")),null);
		if(v.compareTo(min)<0||v.compareTo(max)>0)throw new ExcecaoNegocio(CodigoErro.VALOR_FORA_DO_INTERVALO,"RN-AVA-03","Valor fora do intervalo permitido.",
			List.of(new ErroCampoResposta(campo,"VALOR_FORA_DO_INTERVALO","Informe um valor entre "+min.stripTrailingZeros().toPlainString()+" e "+max.stripTrailingZeros().toPlainString()+".")),null);
	}
}
