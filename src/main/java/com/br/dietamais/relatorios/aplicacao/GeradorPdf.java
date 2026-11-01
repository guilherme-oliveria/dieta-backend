package com.br.dietamais.relatorios.aplicacao;

import com.br.dietamais.avaliacoes.api.*;
import com.br.dietamais.dietas.api.*;
import com.br.dietamais.referencias.api.PorcoesGrupos;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Monta o XHTML e converte em PDF (openhtmltopdf). Sem acesso a banco: recebe tudo pronto. */
public class GeradorPdf {
	static final ZoneId FUSO=ZoneId.of("America/Sao_Paulo");
	private static final Locale PT_BR=Locale.of("pt","BR");
	private static final DateTimeFormatter DATA=DateTimeFormatter.ofPattern("dd/MM/yyyy",PT_BR);
	private static final DateTimeFormatter DATA_HORA=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",PT_BR);
	private static final DateTimeFormatter HORA=DateTimeFormatter.ofPattern("HH:mm");

	public record Logo(byte[] conteudo,String tipo) { }
	public record Cabecalho(String nutricionista,String crn,String paciente,Logo logo) { }

	public byte[] dieta(Cabecalho c,DietaSnapshotResposta d){
		var h=new StringBuilder();
		cabecalho(h,c,"Plano alimentar");
		h.append("<h2>").append(esc(d.titulo())).append("</h2>");
		h.append("<p class=\"meta\">Publicada em ").append(DATA.format(d.publicadoEm().atZone(FUSO))).append("</p>");
		if(d.metas()!=null){
			var m=d.metas();
			h.append("<p class=\"meta\">Meta: ").append(num(m.vetKcal(),0)).append(" kcal | carboidratos ").append(num(m.carboidratoPct(),0)).append("% | proteínas ")
				.append(num(m.proteinaPct(),0)).append("% | lipídios ").append(num(m.lipidioPct(),0)).append("%</p>");
		}
		var porNome=new HashMap<UUID,String>();d.refeicoes().forEach(r->porNome.put(r.id(),r.nome()));
		for(var r:d.refeicoes().stream().sorted(Comparator.comparingInt(RefeicaoResposta::ordem)).toList()){
			h.append("<div class=\"bloco\"><h3>").append(HORA.format(r.horario())).append(" - ").append(esc(r.nome()));
			if(r.alternativa())h.append(" (alternativa").append(porNome.containsKey(r.alternativaDe())?" a "+esc(porNome.get(r.alternativaDe())):"").append(")");
			h.append("</h3>");
			h.append("<table><thead><tr><th>Alimento</th><th class=\"n\">Porções</th><th>Medida caseira</th><th class=\"n\">Gramas</th><th class=\"n\">kcal</th><th>Substituições</th></tr></thead><tbody>");
			for(var i:r.itens()){
				h.append("<tr><td>").append(esc(i.textoLivre()!=null?i.textoLivre():i.nomeAlimento()));
				if(i.observacao()!=null&&!i.observacao().isBlank())h.append("<div class=\"obs\">").append(esc(i.observacao())).append("</div>");
				h.append("</td><td class=\"n\">").append(i.porcoes()==null?"":num(i.porcoes(),2)).append("</td><td>").append(esc(i.ehAVontade()?"à vontade":i.medidaCaseira())).append("</td><td class=\"n\">")
					.append(i.gramas()==null?"":num(i.gramas(),0)).append("</td><td class=\"n\">").append(i.ehAVontade()?"":num(i.totais().kcal(),0)).append("</td><td class=\"obs\">")
					.append(substituicao(i)).append("</td></tr>");
			}
			h.append("</tbody></table><p class=\"total\">").append(totais(r.totais())).append("</p></div>");
		}
		h.append("<div class=\"bloco\"><h3>Total do dia</h3><p class=\"total\">").append(totais(d.totais())).append("</p>");
		if(d.totais().saldoMetas()!=null){
			var s=d.totais().saldoMetas();
			h.append("<p class=\"total\">Saldo da meta: ").append(num(s.kcal(),0)).append(" kcal | carboidratos ").append(num(s.carboidrato(),1)).append(" g | proteínas ")
				.append(num(s.proteina(),1)).append(" g | gorduras ").append(num(s.lipidio(),1)).append(" g</p>");
		}
		h.append("</div>");
		if(d.orientacao()!=null&&!d.orientacao().isBlank())h.append("<div class=\"bloco\"><h3>Orientações</h3><p class=\"texto\">").append(esc(d.orientacao())).append("</p></div>");
		if(d.rodape()!=null&&!d.rodape().isBlank())h.append("<div class=\"bloco\"><p class=\"texto\">").append(esc(d.rodape())).append("</p></div>");
		if(d.porcoes()!=null&&!d.porcoes().isEmpty())h.append("<p class=\"obs\">Valores por porção do grupo: ").append(esc(PorcoesGrupos.FONTE)).append("</p>");
		return pdf(h.toString());
	}
	/** RN-DIE-12: a troca é por porção do mesmo grupo (a lista de alimentos do grupo vai no PDF da lista de substituição). */
	static String substituicao(ItemRefeicaoResposta i){
		if(i.ehAVontade()||i.textoLivre()!=null||i.grupoNome()==null||i.porcoes()==null)return "";
		return "1 porção do grupo "+esc(i.grupoNome());
	}

	public byte[] avaliacao(Cabecalho c,AvaliacaoResposta a){
		var h=new StringBuilder();
		cabecalho(h,c,"Avaliação física");
		h.append(PdfAvaliacao.corpo(a));
		return pdf(h.toString(),PdfAvaliacao.CSS,c.nutricionista()+" - "+c.crn());
	}
	static String dataHora(java.time.Instant i){return DATA_HORA.format(i.atZone(FUSO));}

	private void cabecalho(StringBuilder h,Cabecalho c,String titulo){
		h.append("<div class=\"topo\">");
		if(c.logo()!=null)h.append("<img class=\"logo\" src=\"data:").append(c.logo().tipo()).append(";base64,").append(Base64.getEncoder().encodeToString(c.logo().conteudo())).append("\" />");
		h.append("<div class=\"quem\"><strong>").append(esc(c.nutricionista())).append("</strong><br/>").append(esc(c.crn())).append("</div></div>");
		h.append("<h1>").append(esc(titulo)).append("</h1>");
		if(c.paciente()!=null)h.append("<p class=\"meta\">Paciente: <strong>").append(esc(c.paciente())).append("</strong></p>");
	}
	private void linha(StringBuilder h,String rotulo,String valor){h.append("<tr><td>").append(esc(rotulo)).append("</td><td class=\"n\">").append(esc(valor)).append("</td></tr>");}
	private String totais(TotaisNutricionaisResposta t){
		return "Total: "+num(t.kcal(),0)+" kcal | carboidratos "+num(t.carboidratos(),1)+" g | proteínas "+num(t.proteinas(),1)+" g | gorduras "+num(t.gorduras(),1)+" g";
	}
	static String classificacao(ClassificacaoImc c){
		return switch(c){case ABAIXO_DO_PESO->"abaixo do peso";case NORMAL->"peso normal";case SOBREPESO->"sobrepeso";case OBESIDADE_I->"obesidade grau I";case OBESIDADE_II->"obesidade grau II";case OBESIDADE_III->"obesidade grau III";};
	}
	private static String n1(BigDecimal v){return v==null?null:num(v,1);}
	static String num(BigDecimal v,int casas){return String.format(PT_BR,"%,."+casas+"f",v.setScale(casas,java.math.RoundingMode.HALF_UP));}
	static String esc(String s){
		if(s==null)return "";
		var o=new StringBuilder(s.length()+8);
		for(char ch:s.toCharArray())switch(ch){case '&'->o.append("&amp;");case '<'->o.append("&lt;");case '>'->o.append("&gt;");case '"'->o.append("&quot;");case '\''->o.append("&#39;");
			default->{if(ch>=0x20||ch=='\n'||ch=='\t')o.append(ch);}}
		return o.toString().replace("\n","<br/>");
	}

	private static final String CSS="""
		@page { size: A4; margin: 20mm 16mm 22mm 16mm; @bottom-center { content: "Página " counter(page) " de " counter(pages); font-size: 9pt; color: #666; } }
		body { font-family: sans-serif; font-size: 10.5pt; color: #1b1f1c; }
		.topo { border-bottom: 2px solid #2E7D4F; padding-bottom: 8px; margin-bottom: 10px; overflow: hidden; }
		.logo { max-height: 56px; max-width: 160px; float: left; margin-right: 14px; }
		.quem { font-size: 11pt; padding-top: 6px; }
		h1 { font-size: 18pt; color: #2E7D4F; margin: 6px 0 4px 0; }
		h2 { font-size: 14pt; margin: 4px 0; }
		h3 { font-size: 11.5pt; color: #2E7D4F; margin: 14px 0 4px 0; }
		.meta { color: #555; margin: 2px 0 8px 0; }
		.bloco { page-break-inside: avoid; }
		table { width: 100%; border-collapse: collapse; }
		th { text-align: left; font-size: 9pt; color: #555; border-bottom: 1px solid #bbb; padding: 3px 4px; }
		td { padding: 3px 4px; border-bottom: 1px solid #e3e6e0; }
		.n { text-align: right; }
		.obs { font-size: 9pt; color: #666; }
		.total { font-size: 9.5pt; color: #333; margin: 4px 0 0 0; }
		.texto { margin: 2px 0; }
		""";

	private byte[] pdf(String corpo){return pdf(corpo,"",null);}
	/** {@code rodape} aparece no canto esquerdo de todas as páginas (nome e CRN da nutri). */
	private byte[] pdf(String corpo,String cssExtra,String rodape){
		String css=rodape==null?"":"@page { @bottom-left { content: \""+rodape.replace("\\","").replace("\"","").replace("\n"," ")+"\"; font-size: 9pt; color: #666; } }";
		String html="<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/><style>"+CSS+cssExtra+css+"</style></head><body>"+corpo+"</body></html>";
		try(var saida=new ByteArrayOutputStream()){
			var b=new PdfRendererBuilder();b.useFastMode();b.withHtmlContent(html,null);b.toStream(saida);b.run();
			return saida.toByteArray();
		}catch(Exception e){throw new IllegalStateException("Falha ao gerar o PDF",e);}
	}
}
