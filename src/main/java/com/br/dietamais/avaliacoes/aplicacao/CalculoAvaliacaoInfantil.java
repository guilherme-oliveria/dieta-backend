package com.br.dietamais.avaliacoes.aplicacao;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.dominio.CalculadoraImc;
import com.br.dietamais.avaliacoes.dominio.infantil.CalculadoraInfantil;
import com.br.dietamais.pacientes.api.*;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Component;

/** RN-CAL-01/04: calcula cada bloco possível, sem extrapolar referências ou inferir entradas ausentes. */
@Component
public class CalculoAvaliacaoInfantil {
	private final CalculoCurvasInfantis curvas;
	private final CalculoFrisanchoAvaliacao frisancho;
	public CalculoAvaliacaoInfantil(CalculoCurvasInfantis curvas,CalculoFrisanchoAvaliacao frisancho){this.curvas=curvas;this.frisancho=frisancho;}

	public static Integer idadeMeses(LocalDate nascimento,Instant dataHora){
		if(nascimento==null||dataHora==null)return null;
		var dia=dataHora.atZone(CalculoAvaliacao.FUSO).toLocalDate();
		return dia.isBefore(nascimento)?null:Math.toIntExact(ChronoUnit.MONTHS.between(nascimento,dia));
	}
	public ResultadosInfantisAvaliacao calcular(SalvarAvaliacaoRequisicao r,FichaParaLista ficha){
		var meses=idadeMeses(ficha.dataNascimento(),r.dataHora());
		if(meses==null||meses>=216)return null;
		int anos=meses/12;
		var entrada=r.infantil();var modo=entrada==null||entrada.modoCurva()==null?ModoCurva.PERCENTIL:entrada.modoCurva();
		String sexo=ficha.sexo()==Sexo.MASCULINO?"masculino":ficha.sexo()==Sexo.FEMININO?"feminino":null;
		var lista=new ArrayList<CurvaCrescimentoAvaliacao>();
		if(meses<120){
			lista.add(curvas.calcular("PESO_IDADE",sexo,meses,r.pesoKg(),modo));
			lista.add(curvas.calcular("ESTATURA_IDADE",sexo,meses,r.estaturaM(),modo));
		}
		var imc=curvas.calcular("IMC_IDADE",sexo,meses,CalculadoraImc.imc(r.pesoKg(),r.estaturaM()),modo);lista.add(imc);
		var motivos=new LinkedHashMap<String,String>();
		Integer pesoNascer=entrada==null?null:entrada.pesoNascerGramas();
		var classeNascer=CalculadoraInfantil.classificarPesoNascer(pesoNascer);
		if(classeNascer==null)motivos.put("classificacaoPesoNascer","FALTA_PESO_NASCER");
		BigDecimal minimo=null,maximo=null;
		if(imc.motivo()==null){
			var cortes=modo==ModoCurva.PERCENTIL?imc.cortesPercentis():imc.cortesEscoresZ();
			minimo=CalculadoraInfantil.pesoParaImc(cortes.get(1),r.estaturaM());
			maximo=CalculadoraInfantil.pesoParaImc(cortes.get(modo==ModoCurva.PERCENTIL?5:4),r.estaturaM());
		}else motivos.put("pesoTeorico",imc.motivo());
		var dia=r.dataHora().atZone(CalculoAvaliacao.FUSO).toLocalDate();
		return new ResultadosInfantisAvaliacao(meses,CalculadoraInfantil.idadeTexto(ficha.dataNascimento(),dia),sexo,modo,
			classeNascer==null?null:classeNascer.name(),minimo,maximo,List.copyOf(lista),braco(r,sexo,anos),
			anos>=10?composicao(r,sexo,anos):null,energia(r,sexo,meses),motivos.isEmpty()?null:Map.copyOf(motivos));
	}
	private List<FrisanchoAvaliacao> braco(SalvarAvaliacaoRequisicao r,String sexo,int anos){
		var p=r.perimetros();var d=r.dobras();
		BigDecimal cb=p==null?null:p.braco(),dct=d==null?null:d.triceps(),dcse=d==null?null:d.subescapular();
		BigDecimal ambc=null,agb=null;
		if(sexo!=null){ambc=CalculadoraInfantil.ambc(cb,dct,sexo.equals("masculino"));agb=CalculadoraInfantil.agb(cb,dct,sexo.equals("masculino"));}
		return List.of(frisancho.calcular("CB",sexo,anos,cb),frisancho.calcular("DCT",sexo,anos,dct),frisancho.calcular("DCSE",sexo,anos,dcse),
			frisancho.calcular("AMBC",sexo,anos,ambc),frisancho.calcular("AGB",sexo,anos,agb));
	}
	private static ComposicaoInfantil composicao(SalvarAvaliacaoRequisicao r,String sexo,int anos){
		String motivo=null;BigDecimal pct=null;
		var d=r.dobras();
		if(sexo==null)motivo="FALTA_SEXO";
		else if(d==null||d.triceps()==null||d.subescapular()==null)motivo="FALTA_DOBRAS";
		else{
			var etnia=r.infantil()==null?null:r.infantil().etniaSlaughter();
			pct=CalculadoraInfantil.slaughter(sexo.equals("masculino"),d.triceps(),d.subescapular(),anos,etnia==null?null:etnia==EtniaSlaughter.BRANCA?1:2);
			if(pct==null)motivo="FALTA_ETNIA_SLAUGHTER";
			else if(pct.signum()<0||pct.compareTo(new BigDecimal("100"))>0){pct=null;motivo="RESULTADO_FORA_DO_INTERVALO";}
		}
		if(pct==null)return new ComposicaoInfantil(null,null,null,null,null,Map.of("percentualGordura",motivo));
		var magro=new BigDecimal("100").subtract(pct);
		var gordo=r.pesoKg().multiply(pct).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);
		return new ComposicaoInfantil(pct,CalculadoraInfantil.classificarGordura(sexo.equals("masculino"),pct).name(),magro,gordo,r.pesoKg().subtract(gordo).setScale(2,RoundingMode.HALF_UP),null);
	}
	private static EnergiaInfantil energia(SalvarAvaliacaoRequisicao r,String sexo,int meses){
		if(sexo==null)return new EnergiaInfantil(null,null,null,Map.of("energia","FALTA_SEXO"));
		int anos=meses/12;boolean masculino=sexo.equals("masculino");
		var fator=r.infantil()==null?null:r.infantil().fatorAtividade();
		var geb=CalculadoraInfantil.gebIom(masculino,BigDecimal.valueOf(anos),r.pesoKg(),r.estaturaM());
		var iom=CalculadoraInfantil.getIom(masculino,anos,fator,r.pesoKg(),r.estaturaM());
		var rdi=CalculadoraInfantil.getRdi(masculino,anos,meses%12,fator,r.pesoKg(),r.estaturaM());
		var motivos=new LinkedHashMap<String,String>();
		if(iom==null)motivos.put("getIomKcal",anos<3?"IDADE_FORA_DA_FORMULA":"FALTA_FATOR_ATIVIDADE");
		if(rdi==null)motivos.put("getRdiKcal","FALTA_FATOR_ATIVIDADE");
		return new EnergiaInfantil(geb,iom,rdi,motivos.isEmpty()?null:Map.copyOf(motivos));
	}
}
