package com.br.dietamais.relatorios.aplicacao;

import static com.br.dietamais.relatorios.aplicacao.GeradorPdf.esc;
import static com.br.dietamais.relatorios.aplicacao.GeradorPdf.num;
import com.br.dietamais.avaliacoes.api.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Corpo do PDF da avaliação no layout da planilha da Nut. Luciana (BE-46): medidas e peso teórico, perímetros com cintura, dobras com somatório,
 * composição completa, energia e metas, tabelas de referência, texto-resumo e 4 gráficos (peso × faixa teórica, perímetros, dobras e rosca da
 * composição). Barras em HTML/CSS; só a rosca é imagem. Seção sem dado fica de fora (RN-CAL-04) e o que o paciente não pode ver já vem filtrado.
 */
final class PdfAvaliacao {
	private static final String[] CORES={"#2E7D4F","#C2551F","#4F83B8","#B9A23A","#8A6BB1"};
	private PdfAvaliacao(){}

	static final String CSS="""
		.barra-linha { margin: 2px 0; font-size: 9pt; }
		.barra-rotulo { display: inline-block; width: 27%; }
		.barra-trilho { display: inline-block; width: 52%; background: #eef0ea; height: 9px; }
		.barra-cheia { display: block; background: #2E7D4F; height: 9px; }
		.barra-valor { display: inline-block; width: 18%; text-align: right; }
		.faixa-trilho { position: relative; background: #eef0ea; height: 16px; margin: 6px 0 2px 0; }
		.faixa-ideal { position: absolute; top: 0; height: 16px; background: #b9dcc6; }
		.faixa-marca { position: absolute; top: -3px; width: 3px; height: 22px; background: #C2551F; }
		.legenda { font-size: 9pt; margin: 0; }
		.pt { display: inline-block; width: 9px; height: 9px; margin-right: 4px; }
		.rosca { width: 150px; height: 150px; }
		.nota { font-size: 8.5pt; color: #666; }
		.resumo { background: #f5f6f1; padding: 6px 8px; }
		""";

	static String corpo(AvaliacaoResposta a){
		var h=new StringBuilder();var r=a.resultados();
		linhaMeta(h,a);
		medidas(h,a,r);
		pesoTeorico(h,a,r);
		perimetros(h,a,r);
		dobras(h,a,r);
		composicao(h,a,r);
		if(a.energiaResultado()!=null)energia(h,a.energiaResultado());
		referencias(h);
		if(r!=null&&r.textoResumo()!=null&&!r.textoResumo().isBlank())h.append("<div class=\"bloco\"><h3>Resumo</h3><p class=\"texto resumo\">").append(esc(r.textoResumo())).append("</p></div>");
		if(a.parecer()!=null&&!a.parecer().isBlank())h.append("<div class=\"bloco\"><h3>Parecer</h3><p class=\"texto\">").append(esc(a.parecer())).append("</p></div>");
		return h.toString();
	}

	private static void linhaMeta(StringBuilder h,AvaliacaoResposta a){
		h.append("<p class=\"meta\">Data: ").append(GeradorPdf.dataHora(a.dataHora()));
		if(a.resultados()!=null&&a.resultados().idadeAnos()!=null)h.append(" | Idade: ").append(a.resultados().idadeAnos()).append(" anos");
		if(a.protocolo()!=null)h.append(" | Protocolo: ").append(a.protocolo().dobras()==ProtocoloDobras.TRES?"3 dobras":"7 dobras").append(", massa ").append(a.protocolo().massa()==ProtocoloMassa.MUSCULAR?"muscular":"magra");
		h.append("</p>");
	}

	private static void medidas(StringBuilder h,AvaliacaoResposta a,ResultadosAvaliacao r){
		var l=new StringBuilder();
		linha(l,"Peso",num(a.pesoKg(),1)+" kg");linha(l,"Estatura",num(a.estaturaM(),2)+" m");
		linha(l,"IMC",num(a.imc(),1)+" kg/m² ("+(r!=null&&r.classificacaoImc()!=null?rotulo(r.classificacaoImc()):GeradorPdf.classificacao(a.classificacaoImc()))+")");
		if(r!=null&&r.riscoComorbidades()!=null)linha(l,"Risco de comorbidades pelo IMC",rotulo(r.riscoComorbidades()));
		if(r!=null&&r.compleicao()!=null)linha(l,"Compleição óssea",rotulo(r.compleicao()));
		var c=a.complementares();
		if(c!=null){
			if(c.pesoUsualKg()!=null)linha(l,"Peso usual",num(c.pesoUsualKg(),1)+" kg");
			if(c.pesoDesejadoKg()!=null)linha(l,"Peso desejado",num(c.pesoDesejadoKg(),1)+" kg");
		}
		h.append("<div class=\"bloco\"><h3>Medidas</h3><table><tbody>").append(l).append("</tbody></table></div>");
	}

	/** Gráfico 1: o peso atual sobre a faixa de peso teórico (IMC 18,5–24,9). */
	private static void pesoTeorico(StringBuilder h,AvaliacaoResposta a,ResultadosAvaliacao r){
		if(r==null||r.pesoTeoricoMinKg()==null||r.pesoTeoricoMaxKg()==null)return;
		double min=r.pesoTeoricoMinKg().doubleValue(),max=r.pesoTeoricoMaxKg().doubleValue(),peso=a.pesoKg().doubleValue();
		double de=Math.max(0,Math.min(min,peso)-10),ate=Math.max(max,peso)+10,larg=ate-de;
		h.append("<div class=\"bloco\"><h3>Peso e faixa teórica</h3><p class=\"legenda\">Faixa teórica: ").append(num(r.pesoTeoricoMinKg(),1)).append(" a ").append(num(r.pesoTeoricoMaxKg(),1))
			.append(" kg | Peso atual: ").append(num(a.pesoKg(),1)).append(" kg</p><div class=\"faixa-trilho\"><div class=\"faixa-ideal\" style=\"left:").append(pct((min-de)/larg))
			.append("%;width:").append(pct((max-min)/larg)).append("%\"></div><div class=\"faixa-marca\" style=\"left:").append(pct((peso-de)/larg)).append("%\"></div></div>");
		h.append("<p class=\"nota\">Faixa em verde, peso atual em laranja.</p></div>");
	}

	private static void perimetros(StringBuilder h,AvaliacaoResposta a,ResultadosAvaliacao r){
		var p=a.perimetros();
		var itens=new LinkedHashMap<String,BigDecimal>();
		itens.put("Pescoço",p.pescoco());itens.put("Tórax",p.torax());itens.put("Cintura",p.cintura());itens.put("Abdômen",p.abdomen());itens.put("Quadril",p.quadril());
		itens.put("Braço",p.braco());itens.put("Antebraço",p.antebraco());itens.put("Coxa",p.coxa());itens.put("Panturrilha",p.panturrilha());
		barras(h,"Perímetros (cm)",itens,r!=null&&r.classificacaoCintura()!=null?"Risco pela cintura: "+rotulo(r.classificacaoCintura()):null,"cm");
	}
	private static void dobras(StringBuilder h,AvaliacaoResposta a,ResultadosAvaliacao r){
		var d=a.dobras();
		var itens=new LinkedHashMap<String,BigDecimal>();
		itens.put("Tríceps",d.triceps());itens.put("Subescapular",d.subescapular());itens.put("Peitoral",d.peitoral());itens.put("Axilar média",d.axilarMedia());
		itens.put("Suprailíaca",d.suprailiaca());itens.put("Abdominal",d.abdominal());itens.put("Coxa",d.coxa());
		barras(h,"Dobras cutâneas (mm)",itens,r!=null&&r.somatorioDobrasMm()!=null?"Somatório das dobras: "+num(r.somatorioDobrasMm(),1)+" mm":null,"mm");
	}
	/** Gráficos 2 e 3: tabela e barras proporcionais ao maior valor. */
	private static void barras(StringBuilder h,String titulo,Map<String,BigDecimal> itens,String destaque,String unidade){
		var vazio=itens.values().stream().allMatch(Objects::isNull);
		if(vazio&&destaque==null)return;
		h.append("<div class=\"bloco\"><h3>").append(esc(titulo)).append("</h3>");
		if(destaque!=null)h.append("<p class=\"legenda\"><strong>").append(esc(destaque)).append("</strong></p>");
		double maior=itens.values().stream().filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).max().orElse(1);
		itens.forEach((nome,v)->{
			if(v==null)return;
			h.append("<div class=\"barra-linha\"><span class=\"barra-rotulo\">").append(esc(nome)).append("</span><span class=\"barra-trilho\"><span class=\"barra-cheia\" style=\"width:")
				.append(pct(v.doubleValue()/maior)).append("%\"></span></span><span class=\"barra-valor\">").append(num(v,1)).append(" ").append(unidade).append("</span></div>");
		});
		h.append("</div>");
	}

	/** Gráfico 4: rosca da composição (gordo, ósseo, residual e muscular; sem osso e residual, gordo × magro). */
	private static void composicao(StringBuilder h,AvaliacaoResposta a,ResultadosAvaliacao r){
		var k=a.composicao();var linhas=new StringBuilder();
		if(r!=null&&r.densidadeCorporal()!=null)linha(linhas,"Densidade corporal",num(r.densidadeCorporal(),4)+" g/mL");
		var gordura=r!=null&&r.percentualGordura()!=null?r.percentualGordura():k.percentualGordura();
		if(gordura!=null)linha(linhas,"Gordura",num(gordura,1)+" %"+(r!=null&&r.classificacaoPercentualGordura()!=null?" ("+rotulo(r.classificacaoPercentualGordura())+")":""));
		if(r!=null&&r.percentualMagro()!=null)linha(linhas,"Massa magra",num(r.percentualMagro(),1)+" %");
		var gordo=r!=null&&r.pesoGordoKg()!=null?r.pesoGordoKg():k.pesoGordoKg();
		var magro=r!=null&&r.pesoMagroKg()!=null?r.pesoMagroKg():k.pesoMagroKg();
		if(gordo!=null)linha(linhas,"Peso gordo",num(gordo,1)+" kg");
		if(r!=null&&r.pesoOsseoKg()!=null)linha(linhas,"Peso ósseo",num(r.pesoOsseoKg(),1)+" kg");
		if(r!=null&&r.pesoResidualKg()!=null)linha(linhas,"Peso residual",num(r.pesoResidualKg(),1)+" kg");
		if(magro!=null)linha(linhas,"Peso magro",num(magro,1)+" kg");
		if(r!=null&&r.pesoParaPercentualDesejadoKg()!=null)linha(linhas,"Peso para o % de gordura desejado",num(r.pesoParaPercentualDesejadoKg(),1)+" kg");
		if(linhas.isEmpty())return;
		h.append("<div class=\"bloco\"><h3>Composição corporal</h3><table><tbody>").append(linhas).append("</tbody></table>");
		var fatias=new LinkedHashMap<String,BigDecimal>();
		if(gordo!=null&&magro!=null){
			fatias.put("Gordo",gordo);
			if(r!=null&&r.pesoOsseoKg()!=null&&r.pesoResidualKg()!=null){
				var musculo=magro.subtract(r.pesoOsseoKg()).subtract(r.pesoResidualKg());
				if(musculo.signum()>0){fatias.put("Muscular",musculo);fatias.put("Ósseo",r.pesoOsseoKg());fatias.put("Residual",r.pesoResidualKg());}else fatias.put("Magro",magro);
			}else fatias.put("Magro",magro);
		}
		var imagem=fatias.size()>=2?rosca(fatias.values().stream().map(BigDecimal::doubleValue).toList()):null;
		if(imagem!=null){
			h.append("<p><img class=\"rosca\" src=\"data:image/png;base64,").append(Base64.getEncoder().encodeToString(imagem)).append("\" /></p><p class=\"legenda\">");
			int i=0;var total=fatias.values().stream().mapToDouble(BigDecimal::doubleValue).sum();
			for(var e:fatias.entrySet()){
				h.append("<span class=\"pt\" style=\"background:").append(CORES[i++%CORES.length]).append("\"></span>").append(esc(e.getKey())).append(" ").append(num(e.getValue(),1)).append(" kg (")
					.append(num(BigDecimal.valueOf(e.getValue().doubleValue()/total*100),1)).append("%)&#160;&#160; ");
			}
			h.append("</p>");
		}
		h.append("</div>");
	}
	/** PNG da rosca sem texto (só arcos); {@code null} se o ambiente não tiver AWT, e o PDF sai sem a imagem. */
	static byte[] rosca(List<Double> valores){
		try{
			int t=300;var img=new BufferedImage(t,t,BufferedImage.TYPE_INT_ARGB);var g=img.createGraphics();
			try{
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
				double total=valores.stream().mapToDouble(Double::doubleValue).sum(),ang=90;int i=0;
				for(double v:valores){
					double ext=-360*v/total;
					g.setColor(Color.decode(CORES[i++%CORES.length]));g.fill(new Arc2D.Double(10,10,t-20,t-20,ang,ext,Arc2D.PIE));ang+=ext;
				}
				g.setComposite(AlphaComposite.Clear);g.fillOval(75,75,t-150,t-150);
			}finally{g.dispose();}
			try(var saida=new ByteArrayOutputStream()){ImageIO.write(img,"png",saida);return saida.toByteArray();}
		}catch(Exception|LinkageError e){return null;}
	}

	private static void energia(StringBuilder h,EnergiaResultado e){
		var l=new StringBuilder();
		if(e.gebKcal()!=null)linha(l,"Gasto energético basal (GEB)",num(e.gebKcal(),0)+" kcal");
		if(e.getKcal()!=null)linha(l,"Gasto energético total (GET)",num(e.getKcal(),0)+" kcal");
		if(e.vetKcal()!=null)linha(l,"Valor energético total (VET)",num(e.vetKcal(),0)+" kcal");
		if(e.kcalPorKg()!=null)linha(l,"kcal por kg",num(e.kcalPorKg(),1));
		if(l.isEmpty())return;
		h.append("<div class=\"bloco\"><h3>Necessidades de energia</h3><p class=\"legenda\">Método: ").append(metodo(e.metodo())).append("</p><table><tbody>").append(l).append("</tbody></table>");
		var m=new StringBuilder();
		macro(m,"Carboidrato",e.carboidrato());macro(m,"Proteína",e.proteina());macro(m,"Lipídio",e.lipidio());
		if(!m.isEmpty())h.append("<table><thead><tr><th>Meta</th><th class=\"n\">% do VET</th><th class=\"n\">Gramas</th></tr></thead><tbody>").append(m).append("</tbody></table>");
		if(e.colesterolMaximoMg()!=null)h.append("<p class=\"legenda\">Colesterol: no máximo ").append(num(e.colesterolMaximoMg(),0)).append(" mg por dia.</p>");
		h.append("</div>");
	}
	private static void macro(StringBuilder h,String nome,MetaMacro m){
		if(m==null)return;
		h.append("<tr><td>").append(nome).append("</td><td class=\"n\">").append(faixa(m.percentualMinimo(),m.percentualMaximo(),1)).append("</td><td class=\"n\">").append(faixa(m.gramasMinimo(),m.gramasMaximo(),0)).append("</td></tr>");
	}
	private static String faixa(BigDecimal min,BigDecimal max,int casas){
		if(min==null)return "-";
		return max==null||max.compareTo(min)==0?(max==null?"mín. ":"")+num(min,casas):num(min,casas)+" a "+num(max,casas);
	}

	/** Tabelas de referência fixas da planilha: classificação do IMC (OMS) e risco pela cintura. */
	private static void referencias(StringBuilder h){
		h.append("<div class=\"bloco\"><h3>Referências</h3><table><thead><tr><th>IMC (kg/m²) — adulto</th><th>Classificação</th></tr></thead><tbody>")
			.append("<tr><td>Menor que 16</td><td>Magreza grau III</td></tr><tr><td>16 a 16,9</td><td>Magreza grau II</td></tr><tr><td>17 a 18,4</td><td>Magreza grau I</td></tr>")
			.append("<tr><td>18,5 a 24,9</td><td>Peso normal</td></tr><tr><td>25 a 29,9</td><td>Sobrepeso</td></tr><tr><td>30 a 34,9</td><td>Obesidade grau I</td></tr>")
			.append("<tr><td>35 a 39,9</td><td>Obesidade grau II</td></tr><tr><td>40 ou mais</td><td>Obesidade grau III</td></tr></tbody></table>")
			.append("<p class=\"nota\">Idoso (60 anos ou mais): magreza abaixo de 22, peso normal de 22 a 27 e sobrepeso acima de 27.</p></div>");
	}

	private static String metodo(MetodoGet m){
		return switch(m){case FAO_1985->"FAO/OMS 1985";case FAO_2004->"FAO/OMS 2004";case IOM_2005->"IOM 2005";case RDI_2005->"RDI 2005";case MET->"Equivalente metabólico (MET)";};
	}
	private static String rotulo(Enum<?> e){
		return switch(e){
			case ClasseImc c->switch(c){case MG_III->"magreza grau III";case MG_II->"magreza grau II";case MG_I->"magreza grau I";case MG->"magreza";case NO->"peso normal";case SP->"sobrepeso";
				case OB_I->"obesidade grau I";case OB_II->"obesidade grau II";case OB_III->"obesidade grau III";};
			case RiscoComorbidades c->switch(c){case B->"baixo";case ME->"médio";case A->"aumentado";case MO->"moderado";case S->"severo";case MS->"muito severo";};
			case Compleicao c->switch(c){case PEQUENA->"pequena";case MEDIA->"média";case GRANDE->"grande";};
			case ClasseCintura c->switch(c){case B->"baixo";case A->"aumentado";case MA->"muito aumentado";};
			case ClassePercentualGordura c->switch(c){case MT_BXO->"muito baixo";case EXC->"excelente";case MT_BOM->"muito bom";case BOM->"bom";case ADEQ->"adequado";case MOD_ALTO->"moderadamente alto";case ALTO->"alto";case MT_ALTO->"muito alto";};
			default->e.name().toLowerCase();
		};
	}
	private static void linha(StringBuilder h,String rotulo,String valor){h.append("<tr><td>").append(esc(rotulo)).append("</td><td class=\"n\">").append(esc(valor)).append("</td></tr>");}
	private static String pct(double fracao){return String.format(Locale.ROOT,"%.1f",Math.max(0,Math.min(1,fracao))*100);}
}
