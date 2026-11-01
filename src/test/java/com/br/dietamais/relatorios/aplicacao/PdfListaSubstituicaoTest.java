package com.br.dietamais.relatorios.aplicacao;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.referencias.api.*;
import com.br.dietamais.relatorios.api.TamanhoListaPdf;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class PdfListaSubstituicaoTest {
	final UUID grupo=UUID.randomUUID(),id=UUID.randomUUID();
	static BigDecimal n(String v){return new BigDecimal(v);}
	AlimentoResposta alimento(){return new AlimentoResposta(id,null,grupo,"Pão congelado","2 fatias",n("50"),true,0L,Instant.EPOCH);}
	AlimentoParaDieta referencia(){return new AlimentoParaDieta(id,grupo,"Nome alterado","99 fatias",n("900"),true,n("100"),n("20"),n("5"),n("2"),"Pães");}
	PorcoesGrupos referencias(){return new PorcoesGrupos(PorcoesGrupos.FONTE,Map.of(),List.of(new PorcoesGrupos.Grupo(grupo,"Pães",List.of(opcao(3,"150"),opcao(1,"50"),opcao(2,"100")))));}
	PorcoesGrupos.Opcao opcao(int i,String kcal){return new PorcoesGrupos.Opcao(i,n(kcal),n("1"),"porção","1 porção",false,Map.of());}
	@Test void tamanhosEscalamSnapshotECarboidratosSaoDaPorcaoDoGrupo(){
		var lista=new ListaSubstituicaoResposta(UUID.randomUUID(),0L,List.of(alimento()),List.of(),List.of());
		var refs=Map.of(id,referencia());
		var pequena=PdfListaSubstituicao.corpo(lista,refs,referencias(),TamanhoListaPdf.PEQUENA);
		var media=PdfListaSubstituicao.corpo(lista,refs,referencias(),TamanhoListaPdf.MEDIA);
		var grande=PdfListaSubstituicao.corpo(lista,refs,referencias(),TamanhoListaPdf.GRANDE);
		assertThat(pequena).contains("Pão congelado","1 fatias","25","50 kcal").doesNotContain("Carb. (g)","Nome alterado");
		assertThat(media).contains("2 fatias","50","100 kcal");
		assertThat(grande).contains("3 fatias","75","150 kcal");
		assertThat(PdfListaSubstituicao.corpo(lista,refs,referencias(),TamanhoListaPdf.CARBOIDRATOS)).contains("Carb. (g)","20,0","Some","provisórios");
	}
	@Test void grupoSemOpcaoOuReferenciaNaoInventaValores(){
		var lista=new ListaSubstituicaoResposta(UUID.randomUUID(),0L,List.of(alimento()),List.of(),List.of());
		assertThat(PdfListaSubstituicao.corpo(lista,Map.of(),referencias(),TamanhoListaPdf.MEDIA)).contains("omitidos","dados").doesNotContain("Pão congelado");
		var semOpcoes=new PorcoesGrupos("fonte",Map.of(),List.of());
		assertThat(PdfListaSubstituicao.corpo(lista,Map.of(id,referencia()),semOpcoes,TamanhoListaPdf.MEDIA)).contains("omitidos").doesNotContain("Pão congelado");
	}
	@Test void umaUnicaOpcaoRepeteEmTodosOsTamanhosECarboidratoAusenteNaoViraZero(){
		var lista=new ListaSubstituicaoResposta(UUID.randomUUID(),0L,List.of(alimento()),List.of(),List.of());
		var r=referencia();
		var semCarbo=new AlimentoParaDieta(id,grupo,r.nome(),r.medidaCaseira(),r.gramasPorPorcao(),false,r.kcalPorPorcao(),null,r.proteinasPorPorcao(),r.gordurasPorPorcao(),r.grupoNome());
		var unica=new PorcoesGrupos("fonte",Map.of(),List.of(new PorcoesGrupos.Grupo(grupo,"Pães",List.of(opcao(1,"50")))));
		for(var tamanho:TamanhoListaPdf.values())assertThat(PdfListaSubstituicao.corpo(lista,Map.of(id,semCarbo),unica,tamanho)).contains("50 kcal","25","1 fatias");
		assertThat(PdfListaSubstituicao.corpo(lista,Map.of(id,semCarbo),unica,TamanhoListaPdf.CARBOIDRATOS)).contains("Não informado").doesNotContain(">0,0</td>");
	}
	@Test void pdfRenderizaTodasAsVersoesComFonteETextoSeguro() throws Exception {
		var itens=new ArrayList<AlimentoResposta>();itens.add(alimento());
		var refs=new HashMap<UUID,AlimentoParaDieta>();refs.put(id,referencia());
		var grupos=new ArrayList<>(referencias().grupos());
		for(String nome:List.of("Arroz e massas","Feijões","Carne ou ovo","Frutas","Leite e iogurte")){
			var gid=UUID.randomUUID();grupos.add(new PorcoesGrupos.Grupo(gid,nome,List.of(opcao(1,"50"),opcao(2,"100"),opcao(3,"150"))));
			for(int i=0;i<3;i++){
				var aid=UUID.randomUUID();itens.add(new AlimentoResposta(aid,null,gid,"Alimento fictício "+i,"1½ unidade",n("80"),true,0L,Instant.EPOCH));
				refs.put(aid,new AlimentoParaDieta(aid,gid,"Atual","1 unidade",n("80"),true,n("100"),n("20"),n("5"),n("2"),nome));
			}
		}
		var porcoes=new PorcoesGrupos(PorcoesGrupos.FONTE,Map.of(),grupos);
		var lista=new ListaSubstituicaoResposta(UUID.randomUUID(),0L,itens,List.of(),List.of());
		var s=new com.br.dietamais.dietas.api.DietaSnapshotResposta(UUID.randomUUID(),UUID.randomUUID(),lista.pacienteId(),UUID.randomUUID(),
			new com.br.dietamais.pacientes.api.NutricionistaResumoResposta(UUID.randomUUID(),"Nutri fictícia","CRN-1 123"),"Plano",null,List.of(),lista,
			new com.br.dietamais.dietas.api.TotaisNutricionaisResposta(n("0"),n("0"),n("0"),n("0")),Instant.parse("2026-11-01T12:00:00Z"),null,null,null,null);
		var pasta=java.nio.file.Path.of("build/evidencias/BE-50b");java.nio.file.Files.createDirectories(pasta);
		for(var tamanho:TamanhoListaPdf.values()){
			var bytes=new GeradorPdf().lista(new GeradorPdf.Cabecalho("Nutri fictícia","CRN-1 123","Paciente <fictício> & teste",null),s,refs,porcoes,tamanho);
			java.nio.file.Files.write(pasta.resolve(tamanho+".pdf"),bytes);
			try(var doc=org.apache.pdfbox.pdmodel.PDDocument.load(bytes)){
				var texto=new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
				assertThat(texto).contains("Pães","Pão congelado","Paciente <fictício> & teste","D+","CRN-1 123","Página 1 de 1");
				assertThat(doc.getNumberOfPages()).isEqualTo(1);
				javax.imageio.ImageIO.write(new org.apache.pdfbox.rendering.PDFRenderer(doc).renderImageWithDPI(0,110),"png",pasta.resolve(tamanho+".png").toFile());
			}
		}
	}
	@Test void listaLongaMantemTodosOsAlimentosEntrePaginas() throws Exception {
		var itens=new ArrayList<AlimentoResposta>();var refs=new HashMap<UUID,AlimentoParaDieta>();
		for(int i=0;i<80;i++){
			var a=new AlimentoResposta(UUID.randomUUID(),null,grupo,"Alimento fictício número "+i,"2 fatias",n("50"),true,0L,Instant.EPOCH);
			itens.add(a);var r=referencia();refs.put(a.id(),new AlimentoParaDieta(a.id(),grupo,r.nome(),r.medidaCaseira(),r.gramasPorPorcao(),true,r.kcalPorPorcao(),r.carboidratosPorPorcao(),r.proteinasPorPorcao(),r.gordurasPorPorcao(),r.grupoNome()));
		}
		var lista=new ListaSubstituicaoResposta(UUID.randomUUID(),0L,itens,List.of(),List.of());
		var s=new com.br.dietamais.dietas.api.DietaSnapshotResposta(UUID.randomUUID(),UUID.randomUUID(),lista.pacienteId(),UUID.randomUUID(),
			new com.br.dietamais.pacientes.api.NutricionistaResumoResposta(UUID.randomUUID(),"Nutri fictícia","CRN-1 123"),"Plano",null,List.of(),lista,
			new com.br.dietamais.dietas.api.TotaisNutricionaisResposta(n("0"),n("0"),n("0"),n("0")),Instant.EPOCH,null,null,null,null);
		var bytes=new GeradorPdf().lista(new GeradorPdf.Cabecalho("Nutri fictícia","CRN-1 123","Paciente fictício",null),s,refs,referencias(),TamanhoListaPdf.CARBOIDRATOS);
		try(var doc=org.apache.pdfbox.pdmodel.PDDocument.load(bytes)){
			assertThat(doc.getNumberOfPages()).isGreaterThan(1);
			var texto=new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
			for(var item:itens)assertThat(texto.replaceAll("\\s+"," ")).contains(item.nome());
			var pasta=java.nio.file.Path.of("build/evidencias/BE-50b");java.nio.file.Files.createDirectories(pasta);
			java.nio.file.Files.write(pasta.resolve("LISTA-LONGA.pdf"),bytes);
			for(int i=0;i<doc.getNumberOfPages();i++)javax.imageio.ImageIO.write(new org.apache.pdfbox.rendering.PDFRenderer(doc).renderImageWithDPI(i,90),"png",pasta.resolve("LISTA-LONGA-"+i+".png").toFile());
		}
	}
}
