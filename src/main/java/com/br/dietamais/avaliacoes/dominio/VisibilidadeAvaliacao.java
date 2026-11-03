package com.br.dietamais.avaliacoes.dominio;

import com.br.dietamais.avaliacoes.api.*;
import java.util.*;

/**
 * RN-RES-01: o paciente só vê as seções que a nutri compartilhou. Chave nula = padrão (tudo, exceto exames).
 * Peso, estatura e IMC acompanham a avaliação compartilhada. Seção escondida some do JSON (nunca vira zero).
 */
public final class VisibilidadeAvaliacao {
	private VisibilidadeAvaliacao(){}
	private static boolean ver(Boolean chave){return chave==null||chave;}

	public static AvaliacaoResposta paraPaciente(AvaliacaoResposta a){
		var c=a.compartilhamento()==null?new CompartilhamentoAvaliacao(null,null,null,null,null):a.compartilhamento();
		boolean medidas=ver(c.medidas()),composicao=ver(c.composicao()),necessidades=ver(c.necessidades()),resumo=ver(c.resumo());
		var r=a.resultados();
		if(r!=null)r=filtrar(r,medidas,composicao,resumo);
		return new AvaliacaoResposta(a.id(),a.pacienteId(),a.vinculoId(),a.dataHora(),a.pesoKg(),a.estaturaM(),a.imc(),a.classificacaoImc(),
			medidas?a.perimetros():PerimetrosAvaliacao.VAZIO,medidas?a.dobras():DobrasAvaliacao.VAZIO,composicao?a.composicao():ComposicaoAvaliacao.VAZIO,
			resumo?a.parecer():null,a.compartilhada(),a.versao(),a.criadoEm(),a.atualizadoEm(),a.faixaEtaria(),a.protocolo(),
			necessidades?a.complementares():null,r,a.compartilhamento(),necessidades?a.energia():null,necessidades?a.energiaResultado():null,
			c.exames()!=null&&c.exames()?a.exames():null,null,filtrarInfantil(a.resultadosInfantis(),medidas,composicao,necessidades));
	}
	private static ResultadosInfantisAvaliacao filtrarInfantil(ResultadosInfantisAvaliacao r,boolean medidas,boolean composicao,boolean necessidades){
		if(r==null)return null;
		var curvas=r.curvas()==null?null:r.curvas().stream().map(c->new CurvaCrescimentoAvaliacao(c.indicador(),c.valor(),c.unidade(),c.idadeMesesReferencia(),c.modo(),c.classificacao(),c.percentil(),c.escoreZ(),c.cortesPercentis(),c.cortesEscoresZ(),c.fonte(),c.versaoReferencia(),null)).toList();
		var frisancho=!medidas||r.frisancho()==null?null:r.frisancho().stream().map(f->new FrisanchoAvaliacao(f.indicador(),f.valor(),f.unidade(),f.idadeAnosReferencia(),f.p5(),f.p10(),f.p50(),f.p90(),f.adequacaoPercentual(),f.classificacao(),f.percentil(),f.fonte(),f.versaoReferencia(),null)).toList();
		var c=r.composicao();var e=r.energia();
		return new ResultadosInfantisAvaliacao(r.idadeMeses(),r.idadeTexto(),r.sexoReferencia(),r.modoCurva(),medidas?r.classificacaoPesoNascer():null,r.pesoTeoricoMinKg(),r.pesoTeoricoMaxKg(),curvas,frisancho,
			!composicao||c==null?null:new ComposicaoInfantil(c.percentualGordura(),c.classificacaoPercentualGordura(),c.percentualMagro(),c.pesoGordoKg(),c.pesoMagroKg(),null),
			!necessidades||e==null?null:new EnergiaInfantil(e.gebIomKcal(),e.getIomKcal(),e.getRdiKcal(),null),null);
	}

	private static ResultadosAvaliacao filtrar(ResultadosAvaliacao r,boolean medidas,boolean composicao,boolean resumo){
		return new ResultadosAvaliacao(r.idadeAnos(),r.classificacaoImc(),r.riscoComorbidades(),r.pesoTeoricoMinKg(),r.pesoTeoricoMaxKg(),
			medidas?r.compleicao():null,medidas?r.classificacaoCintura():null,medidas?r.somatorioDobrasMm():null,
			composicao?r.densidadeCorporal():null,composicao?r.percentualGordura():null,composicao?r.classificacaoPercentualGordura():null,composicao?r.percentualMagro():null,
			composicao?r.pesoGordoKg():null,composicao?r.pesoOsseoKg():null,composicao?r.pesoResidualKg():null,composicao?r.pesoMagroKg():null,
			composicao?r.pesoParaPercentualDesejadoKg():null,resumo?r.textoResumo():null,null);
	}
}
