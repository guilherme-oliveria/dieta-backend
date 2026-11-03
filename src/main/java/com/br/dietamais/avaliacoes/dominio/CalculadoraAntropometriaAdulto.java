package com.br.dietamais.avaliacoes.dominio;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.referencias.api.ConsultaReferencias;
import java.math.*;
import java.util.*;

/**
 * Porta das macros da planilha da Nut. Luciana para adulto e idoso (plano das planilhas, seção 3.1) com as correções da
 * seção 3.6. Classificações vêm das tabelas de referência (BE-39); as fórmulas são funções puras com {@link BigDecimal}.
 * Kg e % com 2 casas e densidade com 4 (HALF_UP); intermediários sem arredondar; IMC e % de gordura classificados já
 * arredondados em 2 casas. RN-CAL-01, RN-CAL-04 (resultado sem dado fica nulo, com o motivo).
 */
public final class CalculadoraAntropometriaAdulto {
	private static final MathContext MC=new MathContext(24,RoundingMode.HALF_UP);
	private static final BigDecimal CEM=BigDecimal.valueOf(100);
	private final ConsultaReferencias referencias;

	public CalculadoraAntropometriaAdulto(ConsultaReferencias referencias){this.referencias=referencias;}

	public record Resultado(BigDecimal imc,ResultadosAvaliacao resultados){}

	/** {@code INT((data − nascimento)/365,25)}: a fórmula da planilha, mantida para os números baterem. */
	public static int idadeEmAnos(long dias){return (int)Math.floor(dias/365.25);}

	public static BigDecimal imc(BigDecimal pesoKg,BigDecimal estaturaM){return CalculadoraImc.imc(pesoKg,estaturaM);}

	public ClasseImc classificarImc(int idadeAnos,BigDecimal imc){
		return ClasseImc.valueOf(codigo(idadeAnos>=60?"imc_idoso":"imc_adulto",null,idadeAnos,imc));
	}
	public RiscoComorbidades risco(int idadeAnos,BigDecimal imc){
		return RiscoComorbidades.valueOf(codigo(idadeAnos>=60?"risco_imc_idoso":"risco_imc_adulto",null,idadeAnos,imc));
	}
	public ClasseCintura classificarCintura(String sexo,BigDecimal cm){return ClasseCintura.valueOf(codigo("cintura",sexo,null,cm));}
	public Compleicao classificarCompleicao(String sexo,BigDecimal estaturaM,BigDecimal punhoCm){
		return Compleicao.valueOf(codigo("compleicao",sexo,null,estaturaM.multiply(CEM).divide(punhoCm,10,RoundingMode.HALF_UP)));
	}
	/** Recebe o % já arredondado em 2 casas; vazio abaixo de 18 anos (sem tabela). */
	public Optional<ClassePercentualGordura> classificarPercentualGordura(String sexo,int idadeAnos,BigDecimal percentual){
		return referencias.classificar("percentual_gordura_adulto",sexo,idadeAnos,percentual).map(ClassePercentualGordura::valueOf);
	}
	private String codigo(String tabela,String sexo,Integer idade,BigDecimal valor){
		return referencias.classificar(tabela,sexo,idade,valor).orElseThrow(()->new IllegalArgumentException("Sem faixa em "+tabela+" para "+valor));
	}

	public Resultado avaliar(EntradaAvaliacaoAdulto e){
		var nulos=new LinkedHashMap<String,String>();
		var imc=imc(e.pesoKg(),e.estaturaM());
		var classe=classificarImc(e.idadeAnos(),imc);var risco=risco(e.idadeAnos(),imc);
		var e2=e.estaturaM().multiply(e.estaturaM());
		boolean idoso=e.idadeAnos()>=60;
		var minimo=e2.multiply(idoso?BigDecimal.valueOf(22):new BigDecimal("18.5"));
		var maximo=e2.multiply(idoso?BigDecimal.valueOf(27):new BigDecimal("24.9"));
		Compleicao compleicao=null;
		if(e.punhoCm()!=null)compleicao=classificarCompleicao(e.sexo(),e.estaturaM(),e.punhoCm());else nulos.put("compleicao","FALTA_PUNHO");
		ClasseCintura cintura=null;
		if(e.cinturaCm()!=null)cintura=classificarCintura(e.sexo(),e.cinturaCm());else nulos.put("classificacaoCintura","FALTA_CINTURA");

		boolean masculino="masculino".equals(e.sexo());
		var necessarias=e.protocoloDobras()==ProtocoloDobras.SETE?Arrays.asList(e.triceps(),e.peitoral(),e.axilarMedia(),e.subescapular(),e.abdominal(),e.suprailiaca(),e.coxa())
			:masculino?Arrays.asList(e.peitoral(),e.abdominal(),e.coxa()):Arrays.asList(e.triceps(),e.suprailiaca(),e.coxa());
		BigDecimal somatorio=null,densidade=null,percentual=null,pgExato=null,gordo=null,percentualMagro=null;ClassePercentualGordura classePg=null;
		if(necessarias.stream().allMatch(v->v!=null&&v.signum()>0)){
			var s7=soma(e.triceps(),e.peitoral(),e.axilarMedia(),e.subescapular(),e.abdominal(),e.suprailiaca(),e.coxa());
			var dens=densidade(masculino,e.protocoloDobras(),e,s7,BigDecimal.valueOf(e.idadeAnos()));
			pgExato=BigDecimal.valueOf(4.95).divide(dens,MC).subtract(new BigDecimal("4.5")).multiply(CEM);
			somatorio=esc(s7,2);densidade=esc(dens,4);percentual=esc(pgExato,2);
			classePg=classificarPercentualGordura(e.sexo(),e.idadeAnos(),percentual).orElse(null);
			gordo=e.pesoKg().multiply(pgExato).divide(CEM,MC);percentualMagro=esc(CEM.subtract(pgExato),2);
		}else nulos.put("percentualGordura","FALTA_DOBRAS");

		BigDecimal osso=null;
		var motivoOsso=e.diametroPunhoCm()==null?"FALTA_DIAMETRO_PUNHO":"FALTA_DIAMETRO_JOELHO";
		if(e.diametroPunhoCm()!=null&&e.diametroJoelhoCm()!=null){
			// Von Döbeln/Rocha: a potência 0,712 usa double (única operação fora de BigDecimal)
			var base=BigDecimal.valueOf(400).multiply(e2).multiply(e.diametroPunhoCm()).divide(CEM,MC).multiply(e.diametroJoelhoCm()).divide(CEM,MC);
			osso=new BigDecimal(3.02*Math.pow(base.doubleValue(),0.712),MC);
		}else nulos.put("pesoOsseoKg",motivoOsso);
		var residual=(masculino?new BigDecimal("0.241"):new BigDecimal("0.209")).multiply(e.pesoKg());
		BigDecimal magro=null;
		if(gordo==null)nulos.put("pesoMagroKg","FALTA_DOBRAS");
		else if(e.protocoloMassa()==ProtocoloMassa.MAGRO)magro=e.pesoKg().subtract(gordo);
		else if(osso!=null)magro=e.pesoKg().subtract(gordo.add(osso).add(residual));
		else nulos.put("pesoMagroKg",motivoOsso);
		BigDecimal paraPercentual=null;
		if(e.percentualDesejado()!=null&&magro!=null){
			var massa=e.protocoloMassa()==ProtocoloMassa.MAGRO?magro:magro.add(osso==null?BigDecimal.ZERO:osso).add(residual);
			paraPercentual=massa.divide(BigDecimal.ONE.subtract(e.percentualDesejado().divide(CEM,MC)),MC);
		}else nulos.put("pesoParaPercentualDesejadoKg",e.percentualDesejado()==null?"FALTA_PERCENTUAL_DESEJADO":nulos.get("pesoMagroKg"));
		var resultados=new ResultadosAvaliacao(e.idadeAnos(),classe,risco,esc(minimo,2),esc(maximo,2),compleicao,cintura,somatorio,densidade,percentual,classePg,
			percentualMagro,esc(gordo,2),esc(osso,2),esc(residual,2),esc(magro,2),esc(paraPercentual,2),TextoResumoAvaliacao.gerar(classe,risco,cintura,classePg),nulos);
		return new Resultado(imc,resultados);
	}

	private static BigDecimal esc(BigDecimal v,int casas){return v==null?null:v.setScale(casas,RoundingMode.HALF_UP);}
	private static BigDecimal soma(BigDecimal... v){var s=BigDecimal.ZERO;for(var x:v)if(x!=null)s=s.add(x);return s;}
	private static BigDecimal v(BigDecimal x){return x==null?BigDecimal.ZERO:x;}
	private static BigDecimal poli(BigDecimal c,BigDecimal b1,BigDecimal b2,BigDecimal bi,BigDecimal s,BigDecimal idade){
		return c.subtract(b1.multiply(s)).add(b2.multiply(s).multiply(s)).subtract(bi.multiply(idade));
	}
	private static BigDecimal densidade(boolean masculino,ProtocoloDobras p,EntradaAvaliacaoAdulto e,BigDecimal s7,BigDecimal idade){
		if(p==ProtocoloDobras.SETE)return masculino
			?poli(new BigDecimal("1.112"),new BigDecimal("0.00043499"),new BigDecimal("0.00000055"),new BigDecimal("0.0002882"),s7,idade)
			:poli(new BigDecimal("1.097"),new BigDecimal("0.00046971"),new BigDecimal("0.00000056"),new BigDecimal("0.00012828"),s7,idade);
		return masculino
			?poli(new BigDecimal("1.10938"),new BigDecimal("0.0008267"),new BigDecimal("0.0000016"),new BigDecimal("0.0002574"),soma(e.peitoral(),e.abdominal(),e.coxa()),idade)
			:poli(new BigDecimal("1.0994921"),new BigDecimal("0.0009929"),new BigDecimal("0.0000023"),new BigDecimal("0.0001392"),soma(e.triceps(),e.suprailiaca(),e.coxa()),idade);
	}
}
