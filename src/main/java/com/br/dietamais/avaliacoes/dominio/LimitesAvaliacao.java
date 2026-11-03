package com.br.dietamais.avaliacoes.dominio;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.comum.api.*;
import java.math.BigDecimal;
import java.util.List;

/** RN-AVA-03: limites de sanidade. Fora do intervalo = 422; precisão excessiva = 400 (estatura pediátrica: 3 casas; demais: 2). */
public final class LimitesAvaliacao {
	private LimitesAvaliacao(){}
	private static final BigDecimal PESO_MIN=new BigDecimal("20"),PESO_MAX=new BigDecimal("400"),EST_MIN=new BigDecimal("0.5"),EST_MAX=new BigDecimal("2.5");
	private static final BigDecimal PER_MIN=new BigDecimal("10"),PER_MAX=new BigDecimal("250"),DOB_MIN=BigDecimal.ONE,DOB_MAX=new BigDecimal("80"),PERC_MAX=new BigDecimal("100");

	public static void validar(SalvarAvaliacaoRequisicao r){
		validar(r,false);
	}
	/** A faixa vem da ficha autenticada e da data da avaliação, nunca de uma escolha no corpo. */
	public static void validar(SalvarAvaliacaoRequisicao r,boolean infantil){
		var pesoMin=infantil?new BigDecimal("0.1"):PESO_MIN;
		var perMin=infantil?BigDecimal.ONE:PER_MIN;
		var dobraMin=infantil?new BigDecimal("0.1"):DOB_MIN;
		faixa("pesoKg",r.pesoKg(),pesoMin,PESO_MAX);faixa("estaturaM",r.estaturaM(),infantil?new BigDecimal("0.2"):EST_MIN,EST_MAX,infantil?3:2);
		var p=r.perimetros()==null?PerimetrosAvaliacao.VAZIO:r.perimetros();
		opcional("perimetros.pescoco",p.pescoco(),perMin,PER_MAX);opcional("perimetros.torax",p.torax(),perMin,PER_MAX);opcional("perimetros.cintura",p.cintura(),perMin,PER_MAX);
		opcional("perimetros.abdomen",p.abdomen(),perMin,PER_MAX);opcional("perimetros.quadril",p.quadril(),perMin,PER_MAX);opcional("perimetros.braco",p.braco(),perMin,PER_MAX);
		opcional("perimetros.antebraco",p.antebraco(),perMin,PER_MAX);opcional("perimetros.coxa",p.coxa(),perMin,PER_MAX);opcional("perimetros.panturrilha",p.panturrilha(),perMin,PER_MAX);
		var d=r.dobras()==null?DobrasAvaliacao.VAZIO:r.dobras();
		opcional("dobras.triceps",d.triceps(),dobraMin,DOB_MAX);opcional("dobras.subescapular",d.subescapular(),dobraMin,DOB_MAX);opcional("dobras.peitoral",d.peitoral(),dobraMin,DOB_MAX);
		opcional("dobras.axilarMedia",d.axilarMedia(),dobraMin,DOB_MAX);opcional("dobras.suprailiaca",d.suprailiaca(),dobraMin,DOB_MAX);opcional("dobras.abdominal",d.abdominal(),dobraMin,DOB_MAX);
		opcional("dobras.coxa",d.coxa(),dobraMin,DOB_MAX);
		var c=r.composicao()==null?ComposicaoAvaliacao.VAZIO:r.composicao();
		opcional("composicao.percentualGordura",c.percentualGordura(),BigDecimal.ZERO,PERC_MAX);opcional("composicao.pesoGordoKg",c.pesoGordoKg(),BigDecimal.ZERO,PESO_MAX);
		opcional("composicao.pesoMagroKg",c.pesoMagroKg(),BigDecimal.ZERO,PESO_MAX);
		var m=r.complementares();
		if(m!=null){
			opcional("complementares.pesoUsualKg",m.pesoUsualKg(),pesoMin,PESO_MAX);opcional("complementares.pesoDesejadoKg",m.pesoDesejadoKg(),pesoMin,PESO_MAX);
			opcional("complementares.punhoCm",m.punhoCm(),new BigDecimal("5"),new BigDecimal("40"));
			opcional("complementares.diametroPunhoCm",m.diametroPunhoCm(),BigDecimal.ONE,new BigDecimal("30"));opcional("complementares.diametroJoelhoCm",m.diametroJoelhoCm(),BigDecimal.ONE,new BigDecimal("30"));
			opcional("complementares.percentualGorduraDesejado",m.percentualGorduraDesejado(),new BigDecimal("3"),new BigDecimal("60"));
			opcional("complementares.objetivoKgPorMes",m.objetivoKgPorMes(),BigDecimal.ZERO,BigDecimal.TEN);
		}
		var en=r.energia();
		if(en!=null){
			opcional("energia.colesterolTotal",en.colesterolTotal(),BigDecimal.ZERO,new BigDecimal("1000"));
			var rm=en.rotinaMet();
			if(rm!=null){
				var horas=new BigDecimal("24");
				faixa("energia.rotinaMet.horasSono",rm.horasSono(),BigDecimal.ZERO,horas);faixa("energia.rotinaMet.horasTrabalho",rm.horasTrabalho(),BigDecimal.ZERO,horas);
				if(rm.atividades()!=null)for(int i=0;i<rm.atividades().size();i++)faixa("energia.rotinaMet.atividades["+i+"].minutos",rm.atividades().get(i).minutos(),BigDecimal.ZERO,new BigDecimal("1440"));
			}
		}
	}
	private static void opcional(String campo,BigDecimal v,BigDecimal min,BigDecimal max){if(v!=null)faixa(campo,v,min,max);}
	private static void faixa(String campo,BigDecimal v,BigDecimal min,BigDecimal max){
		faixa(campo,v,min,max,2);
	}
	private static void faixa(String campo,BigDecimal v,BigDecimal min,BigDecimal max,int casas){
		if(v.stripTrailingZeros().scale()>casas){
			String mensagem="Use no máximo "+casas+" casas decimais.";
			throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,null,mensagem,List.of(new ErroCampoResposta(campo,"FORMATO_INVALIDO",mensagem)),null);
		}
		if(v.compareTo(min)<0||v.compareTo(max)>0)throw new ExcecaoNegocio(CodigoErro.VALOR_FORA_DO_INTERVALO,"RN-AVA-03","Valor fora do intervalo permitido.",
			List.of(new ErroCampoResposta(campo,"VALOR_FORA_DO_INTERVALO","Informe um valor entre "+min.stripTrailingZeros().toPlainString()+" e "+max.stripTrailingZeros().toPlainString()+".")),null);
	}
}
