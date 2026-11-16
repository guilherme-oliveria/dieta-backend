package com.br.dietamais.avaliacoes.aplicacao;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.avaliacoes.dominio.infantil.CalculadoraInfantil;
import com.br.dietamais.avaliacoes.dominio.infantil.CortesCrescimento;
import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CalculoFrisanchoAvaliacao {
	private static final String FONTE="Frisancho 1981/1990 (planilha da Nut. Luciana: aba outras)",VERSAO="frisancho-1.0.0";
	private static final List<BigDecimal> POSICOES=List.of(new BigDecimal("5"),BigDecimal.TEN,new BigDecimal("50"),new BigDecimal("90"));
	private final ConsultaFrisancho referencias;
	public CalculoFrisanchoAvaliacao(ConsultaFrisancho referencias){this.referencias=referencias;}

	public FrisanchoAvaliacao calcular(String indicador,String sexo,int idadeAnos,BigDecimal valor){
		if(!PercentisFrisancho.INDICADORES.contains(indicador))throw new IllegalArgumentException("Indicador Frisancho desconhecido");
		boolean area=indicador.equals("AMBC")||indicador.equals("AGB");
		String unidade=area?"mm²":indicador.equals("CB")?"cm":"mm";
		if(!"masculino".equals(sexo)&&!"feminino".equals(sexo))return resultado(indicador,valor,unidade,null,null,null,null,"FALTA_SEXO");
		if(valor==null)return resultado(indicador,null,unidade,null,null,null,null,"FALTA_MEDIDA");
		var ref=referencias.percentis(indicador,sexo,idadeAnos).orElse(null);
		if(ref==null)return resultado(indicador,valor,unidade,null,null,null,null,"REFERENCIA_INDISPONIVEL");
		if(!area){
			var adequacao=CalculadoraInfantil.adequacao(valor,ref.p50());
			return resultado(indicador,valor,unidade,ref,adequacao,adequacao==null?null:CalculadoraInfantil.classificarAdequacao(adequacao).name(),null,adequacao==null?"REFERENCIA_INCONSISTENTE":null);
		}
		var intervalo=CortesCrescimento.localizar(valor,Arrays.asList(ref.p5(),ref.p10(),ref.p50(),ref.p90()),POSICOES).orElse(null);
		if(intervalo==null){
			boolean g16=indicador.equals("AMBC")&&sexo.equals("masculino")&&idadeAnos==4;
			return resultado(indicador,valor,unidade,ref,null,null,null,g16?"REFERENCIA_INCONSISTENTE_G16":"REFERENCIA_INCONSISTENTE");
		}
		String classe=CalculadoraInfantil.classificarPercentilBraco(valor,ref.p5(),ref.p10(),ref.p90()).name();
		return resultado(indicador,valor,unidade,ref,null,classe,new IntervaloCurva(intervalo.inferior(),intervalo.superior()),null);
	}
	private static FrisanchoAvaliacao resultado(String indicador,BigDecimal valor,String unidade,PercentisFrisancho ref,
			BigDecimal adequacao,String classe,IntervaloCurva percentil,String motivo){
		return new FrisanchoAvaliacao(indicador,valor,unidade,ref==null?null:ref.idadeAnos(),ref==null?null:ref.p5(),ref==null?null:ref.p10(),
			ref==null?null:ref.p50(),ref==null?null:ref.p90(),adequacao,classe,percentil,FONTE,VERSAO,motivo);
	}
}
