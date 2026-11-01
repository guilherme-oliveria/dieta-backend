package com.br.dietamais.referencias;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Confere o CSV das curvas da OMS (BE-54) sem banco, contra as abas P.I., E.I. e I.M.C. das planilhas. */
class OmsCsvTest {
	record Linha(String indicador,String sexo,int meses,double[] v){}
	List<Linha> ler() throws Exception {
		try(var in=getClass().getResourceAsStream("/db/changelog/dados/referencia-oms.csv")){
			var linhas=new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\n");var r=new ArrayList<Linha>();
			for(int i=1;i<linhas.length;i++){var c=linhas[i].split(";",-1);var v=new double[15];for(int j=0;j<15;j++)v[j]=Double.parseDouble(c[3+j]);r.add(new Linha(c[0],c[1],Integer.parseInt(c[2]),v));}
			return r;
		}
	}
	Linha achar(List<Linha> l,String ind,String sexo,int meses){return l.stream().filter(x->x.indicador().equals(ind)&&x.sexo().equals(sexo)&&x.meses()==meses).findFirst().orElseThrow();}

	@Test void tem736LinhasSemRepeticao() throws Exception {
		var l=ler();
		assertThat(l).hasSize(736);
		assertThat(l.stream().map(x->x.indicador()+x.sexo()+x.meses()).distinct()).hasSize(736);
		assertThat(l.stream().filter(x->x.indicador().equals("PESO_IDADE")).count()).isEqualTo(240);
		assertThat(l.stream().filter(x->x.indicador().equals("IMC_IDADE")&&x.meses()>=120).count()).isEqualTo(16);
	}
	@Test void valoresConferidosComAsAbas() throws Exception {
		var l=ler();
		// P.I. feminino, "menos de 1 mês": 0,1° = 2, mediana 3,2, +3 D.P. = 4,8
		var pi=achar(l,"PESO_IDADE","feminino",0).v();assertThat(pi[0]).isEqualTo(2);assertThat(pi[4]).isEqualTo(3.2);assertThat(pi[14]).isEqualTo(4.8);
		// E.I. masculino, 1 mês: mediana 54,7
		assertThat(achar(l,"ESTATURA_IDADE","masculino",1).v()[4]).isEqualTo(54.7);
		// I.M.C. 10 a 17 anos, masculino aos 12 anos: mediana 17,5; 99° 25,2
		var imc=achar(l,"IMC_IDADE","masculino",144).v();assertThat(imc[4]).isEqualTo(17.5);assertThat(imc[7]).isEqualTo(25.2);
	}
	@Test void medianaBateComEscoreZeroEPercentisAPartirDoDecimoSaoCrescentes() throws Exception {
		for(var x:ler()){
			assertThat(Math.abs(x.v()[4]-x.v()[11])).as(x.indicador()+" "+x.sexo()+" "+x.meses()).isLessThanOrEqualTo(0.15);
			for(int i=2;i<7;i++)assertThat(x.v()[i]).as("percentis "+x.indicador()+" "+x.sexo()+" "+x.meses()).isLessThanOrEqualTo(x.v()[i+1]);
			for(int i=8;i<14;i++)assertThat(x.v()[i]).as("Z "+x.indicador()+" "+x.sexo()+" "+x.meses()).isLessThanOrEqualTo(x.v()[i+1]);
		}
	}
	/** Inconsistência da planilha (G-16): de 5 a 10 anos as colunas 0,1° e 3° de peso e estatura andam em degraus anuais e, em 53 linhas, o 3° passa do 10°. Mantemos como está na planilha. */
	@Test void linhasComTerceiroPercentilMaiorQueODecimoSaoAsConhecidasDaPlanilha() throws Exception {
		var fora=ler().stream().filter(x->x.v()[0]>x.v()[1]||x.v()[1]>x.v()[2]).toList();
		assertThat(fora).hasSize(53).allSatisfy(x->assertThat(x.meses()).isBetween(60,119));
	}
}
