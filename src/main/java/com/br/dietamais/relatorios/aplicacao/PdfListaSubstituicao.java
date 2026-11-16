package com.br.dietamais.relatorios.aplicacao;

import static com.br.dietamais.relatorios.aplicacao.GeradorPdf.*;
import com.br.dietamais.alimentos.api.*;
import com.br.dietamais.referencias.api.PorcoesGrupos;
import com.br.dietamais.relatorios.api.TamanhoListaPdf;
import java.math.*;
import java.util.*;

/** Usa a lista publicada; referências atuais fornecem somente energia, macros e nome do grupo. */
final class PdfListaSubstituicao {
	private PdfListaSubstituicao(){}
	static final String CSS="""
		@page { margin: 30pt 30pt 66pt 30pt; @bottom-center { content: element(rodapeLista); } }
		.lista-rodape { position: running(rodapeLista); font-size: 8pt; color: #5B6B61; text-align: center; border-top: 1px solid #E2E6DE; padding-top: 5pt; }
		.lista-pagina:before { content: "Página " counter(page) " de " counter(pages); }
		body { font-family: 'Plus Jakarta Sans'; font-size: 10.5pt; color: #17251D; }
		.marca { background: #2E7D4F; color: white; padding: 6pt; font-size: 16pt; float: left; margin-right: 10pt; }
		.lista-identidade { margin-left: 48pt; }
		.lista-colunas { width: 100%; -fs-table-paginate: paginate; }
		.lista-colunas > tbody > tr > td { width: 50%; vertical-align: top; padding: 0 8pt 12pt 0; border: 0; }
		.lista-grupo { page-break-inside: avoid; }
		.lista-grupo table { -fs-table-paginate: paginate; }
		.lista-grupo h3 { margin-top: 4pt; }
		.lista-grupo th { font-size: 10.5pt; }
		.lista-grupo td { padding: 4pt 2pt; overflow-wrap: break-word; }
		.exemplo { background: #E6F2EA; padding: 10pt; page-break-inside: avoid; }
		""";

	static String corpo(ListaSubstituicaoResposta lista,Map<UUID,AlimentoParaDieta> alimentos,PorcoesGrupos referencias,TamanhoListaPdf tamanho){
		boolean carbo=tamanho==TamanhoListaPdf.CARBOIDRATOS;
		var h=new StringBuilder("<p>Troque um alimento por outro <strong>do mesmo grupo</strong>: cada linha vale <strong>1 porção</strong>, com a mesma energia. Respeite a quantidade de porções da sua dieta.</p>");
		var grupos=new LinkedHashMap<UUID,List<AlimentoResposta>>();
		lista.alimentos().forEach(a->grupos.computeIfAbsent(a.grupoId(),g->new ArrayList<>()).add(a));
		var blocos=new ArrayList<String>();int omitidos=0;
		for(var entrada:grupos.entrySet()){
			var opcoes=referencias.grupos().stream().filter(g->g.grupoId().equals(entrada.getKey())).flatMap(g->g.opcoes().stream())
				.filter(o->o.kcal()!=null&&o.kcal().signum()>0).sorted(Comparator.comparing(PorcoesGrupos.Opcao::kcal).thenComparingInt(PorcoesGrupos.Opcao::opcao)).toList();
			if(opcoes.isEmpty()){omitidos+=entrada.getValue().size();continue;}
			var opcao=opcoes.get(switch(tamanho){case PEQUENA->0;case GRANDE->opcoes.size()-1;default->opcoes.size()/2;});
			var linhas=new StringBuilder();String nome=null;
			for(var a:entrada.getValue()){
				var ref=alimentos.get(a.id());
				if(ref==null||!entrada.getKey().equals(ref.grupoId())||ref.grupoNome()==null||ref.kcalPorPorcao()==null||ref.kcalPorPorcao().signum()<=0){omitidos++;continue;}
				var fator=opcao.kcal().divide(ref.kcalPorPorcao(),12,RoundingMode.HALF_UP);
				nome=ref.grupoNome();
				linhas.append("<tr><td>").append(esc(a.nome())).append("</td><td>").append(esc(EscalaMedidaCaseira.escalar(a.medidaCaseira(),fator)))
					.append("</td><td class=\"n\">").append(num(a.gramasPorPorcao().multiply(fator),0)).append("</td>");
				if(carbo)linhas.append("<td class=\"n\">").append(ref.carboidratosPorPorcao()==null?"Não informado":num(ref.carboidratosPorPorcao().multiply(fator),1)).append("</td>");
				linhas.append("</tr>");
			}
			if(!linhas.isEmpty())blocos.add("<div class=\"lista-grupo\"><h3>"+esc(nome)+" — 1 porção = "+num(opcao.kcal(),0)+" kcal</h3><table><thead><tr><th>Alimento</th><th>Medida caseira</th><th class=\"n\">g</th>"+(carbo?"<th class=\"n\">Carb. (g)</th>":"")+"</tr></thead><tbody>"+linhas+"</tbody></table></div>");
		}
		h.append("<table class=\"lista-colunas\"><tbody>");
		for(int i=0;i<blocos.size();i+=2)h.append("<tr><td>").append(blocos.get(i)).append("</td><td>").append(i+1<blocos.size()?blocos.get(i+1):"").append("</td></tr>");
		h.append("</tbody></table>");
		if(blocos.isEmpty())h.append("<p>Sua nutri ainda não montou sua lista ou faltam referências para exibi-la.</p>");
		h.append("<p class=\"exemplo\"><strong>Exemplo:</strong> se a refeição pede 2 porções de um grupo, escolha um alimento desse grupo e multiplique a medida e as gramas da linha por 2.");
		if(carbo)h.append(" Some os carboidratos da refeição: multiplique o valor de Carb. (g) de cada linha pelo número de porções e some os resultados.");
		h.append("</p><p class=\"obs\">Medidas caseiras arredondadas para ¼; gramas arredondadas para inteiro. Valores por grupo: ").append(esc(PorcoesGrupos.FONTE)).append("</p>");
		if(omitidos>0)h.append("<p class=\"obs\">Alimentos ou grupos omitidos por falta de dados de referência: ").append(omitidos).append(".</p>");
		return h.toString();
	}
}
