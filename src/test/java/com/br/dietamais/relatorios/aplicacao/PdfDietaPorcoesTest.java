package com.br.dietamais.relatorios.aplicacao;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.pacientes.api.NutricionistaResumoResposta;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PdfDietaPorcoesTest {
	static BigDecimal n(String v){return new BigDecimal(v);}
	String texto(byte[] pdf) throws Exception {
		try(var doc=org.apache.pdfbox.pdmodel.PDDocument.load(pdf)){return new org.apache.pdfbox.text.PDFTextStripper().getText(doc);}
	}
	@Test void pdfMostraSubstituicaoAVontadeAlternativaMetasRodapeEFonte() throws Exception {
		UUID grupo=UUID.randomUUID(),principal=UUID.randomUUID();
		var zero=new TotaisNutricionaisResposta(n("0"),n("0"),n("0"),n("0"));
		var totais=new TotaisNutricionaisResposta(n("140.38"),n("24.67"),n("4.01"),n("1.4"));
		var porGrupo=new ItemRefeicaoResposta(UUID.randomUUID(),null,grupo,"Pão",n("2"),"1 fatia",n("100"),null,totais,"Pão",1,null,null);
		var vontade=new ItemRefeicaoResposta(UUID.randomUUID(),null,UUID.randomUUID(),"Vegetal cru ou cozido",null,null,null,null,zero,"Vegetal cru ou cozido",null,Boolean.TRUE,null);
		var livre=new ItemRefeicaoResposta(UUID.randomUUID(),null,null,"Chá de erva-doce",null,null,null,null,zero,null,null,Boolean.TRUE,"Chá de erva-doce");
		var r1=new RefeicaoResposta(principal,"Desjejum",LocalTime.of(7,0),0,List.of(porGrupo,vontade,livre),totais,null,null,null);
		var r2=new RefeicaoResposta(UUID.randomUUID(),"Desjejum 2ª opção",LocalTime.of(7,0),1,List.of(porGrupo),totais,"ALTERNATIVA",principal,null);
		var dia=totais.comSaldo(new SaldoMetasResposta(n("1659.62"),n("200.33"),n("85.99"),n("58.6")));
		var d=new DietaSnapshotResposta(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),new NutricionistaResumoResposta(UUID.randomUUID(),"Nutri fictícia","CRN-1 1"),
			"Plano por porções",null,List.of(r1,r2),null,dia,Instant.parse("2026-11-01T12:00:00Z"),List.of(new PorcaoGrupo(grupo,1)),
			new MetasDieta(n("1800"),n("50"),n("20"),n("30")),null,"Além das refeições acima, verduras à vontade.");
		var t=texto(new GeradorPdf().dieta(new GeradorPdf.Cabecalho("Nutri fictícia","CRN-1 1","Paciente fictício",null),d));
		assertThat(t).contains("Substituições","1 porção do grupo Pão","à vontade","Chá de erva-doce","alternativa a Desjejum","Meta: 1.800 kcal","Saldo da meta","Além das refeições acima","Valores por porção do grupo");
		assertThat(t).doesNotContain("1 porção do grupo Vegetal");
	}
}
