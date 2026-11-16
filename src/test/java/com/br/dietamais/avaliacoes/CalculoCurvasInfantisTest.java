package com.br.dietamais.avaliacoes;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.avaliacoes.api.ModoCurva;
import com.br.dietamais.avaliacoes.aplicacao.CalculoCurvasInfantis;
import com.br.dietamais.referencias.api.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class CalculoCurvasInfantisTest {
	private static List<BigDecimal> numeros(String... valores){return Arrays.stream(valores).map(BigDecimal::new).toList();}
	private final List<String> consultas=new ArrayList<>();
	private final ConsultaOms oms=(indicador,sexo,meses)->{
		consultas.add(indicador+"/"+sexo+"/"+meses);
		if(meses>=120&&!indicador.equals("IMC_IDADE"))return Optional.empty();
		return Optional.of(new CurvaOms(indicador,sexo,meses>=120?meses/12*12:meses,
			numeros("10","12","14","16","18","20","22","24"),numeros("10","12","14","18","19","21","25")));
	};
	private final CalculoCurvasInfantis calculo=new CalculoCurvasInfantis(oms);

	@Test void usaMesesCompletosEConverteMetrosEmCentimetros(){
		var r=calculo.calcular("ESTATURA_IDADE","feminino",24,new BigDecimal("0.19"),ModoCurva.PERCENTIL);
		assertThat(consultas).containsExactly("ESTATURA_IDADE/feminino/24");
		assertThat(r.valor()).isEqualByComparingTo("19");assertThat(r.unidade()).isEqualTo("cm");
		assertThat(r.percentil().inferior()).isEqualByComparingTo("50");assertThat(r.percentil().superior()).isEqualByComparingTo("85");
		assertThat(r.classificacao()).isEqualTo("A");assertThat(r.motivo()).isNull();
		assertThat(r.versaoReferencia()).isEqualTo("oms-1.0.0");assertThat(r.cortesPercentis()).hasSize(8);
	}
	@Test void protocoloEscolhidoMudaClasseMasPreservaAmbasPosicoes(){
		var p=calculo.calcular("IMC_IDADE","masculino",60,new BigDecimal("23"),ModoCurva.PERCENTIL);
		var z=calculo.calcular("IMC_IDADE","masculino",60,new BigDecimal("23"),ModoCurva.ESCORE_Z);
		assertThat(p.classificacao()).isEqualTo("O");assertThat(z.classificacao()).isEqualTo("O");
		assertThat(z.percentil()).isEqualTo(p.percentil());assertThat(z.escoreZ()).isEqualTo(p.escoreZ());
		assertThat(z.escoreZ().inferior()).isEqualByComparingTo("2");assertThat(z.escoreZ().superior()).isEqualByComparingTo("3");
		var diferenca=calculo.calcular("IMC_IDADE","masculino",60,new BigDecimal("21"),ModoCurva.ESCORE_Z);
		assertThat(diferenca.classificacao()).isEqualTo("O");
		assertThat(calculo.calcular("IMC_IDADE","masculino",60,new BigDecimal("21"),ModoCurva.PERCENTIL).classificacao()).isEqualTo("S");
	}
	@Test void adolescenteUsaFaixasPropriasECongelaIdadeDaLinhaAnual(){
		var r=calculo.calcular("IMC_IDADE","feminino",131,new BigDecimal("22"),ModoCurva.PERCENTIL);
		assertThat(r.classificacao()).isEqualTo("OB");assertThat(r.idadeMesesReferencia()).isEqualTo(120);
		var ausente=calculo.calcular("PESO_IDADE","feminino",131,BigDecimal.TEN,ModoCurva.PERCENTIL);
		assertThat(ausente.motivo()).isEqualTo("REFERENCIA_INDISPONIVEL");assertThat(ausente.classificacao()).isNull();
	}
	@Test void naoConsultaSemSexoOuIdadeValida(){
		assertThat(calculo.calcular("IMC_IDADE",null,24,BigDecimal.TEN,ModoCurva.PERCENTIL).motivo()).isEqualTo("FALTA_SEXO");
		assertThat(calculo.calcular("IMC_IDADE","masculino",-1,BigDecimal.TEN,ModoCurva.PERCENTIL).motivo()).isEqualTo("IDADE_FORA_DA_REFERENCIA");
		assertThat(calculo.calcular("IMC_IDADE","masculino",216,BigDecimal.TEN,ModoCurva.PERCENTIL).motivo()).isEqualTo("IDADE_FORA_DA_REFERENCIA");
		assertThat(consultas).isEmpty();
	}
	@Test void cortesInconsistentesNaoGeramClasseNemPosicao(){
		var ruim=new CalculoCurvasInfantis((i,s,m)->Optional.of(new CurvaOms(i,s,m,
			numeros("10","9","14","16","18","20","22","24"),numeros("10","12","14","18","20","22","24"))));
		var r=ruim.calcular("IMC_IDADE","masculino",24,new BigDecimal("15"),ModoCurva.PERCENTIL);
		assertThat(r.motivo()).isEqualTo("REFERENCIA_INCONSISTENTE");assertThat(r.classificacao()).isNull();assertThat(r.percentil()).isNull();
		assertThat(r.cortesPercentis().get(1)).isEqualByComparingTo("9");
	}
}
