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
			necessidades?a.complementares():null,r,a.compartilhamento(),necessidades?a.energia():null,necessidades?a.energiaResultado():null);
	}

	private static ResultadosAvaliacao filtrar(ResultadosAvaliacao r,boolean medidas,boolean composicao,boolean resumo){
		return new ResultadosAvaliacao(r.idadeAnos(),r.classificacaoImc(),r.riscoComorbidades(),r.pesoTeoricoMinKg(),r.pesoTeoricoMaxKg(),
			medidas?r.compleicao():null,medidas?r.classificacaoCintura():null,medidas?r.somatorioDobrasMm():null,
			composicao?r.densidadeCorporal():null,composicao?r.percentualGordura():null,composicao?r.classificacaoPercentualGordura():null,composicao?r.percentualMagro():null,
			composicao?r.pesoGordoKg():null,composicao?r.pesoOsseoKg():null,composicao?r.pesoResidualKg():null,composicao?r.pesoMagroKg():null,
			composicao?r.pesoParaPercentualDesejadoKg():null,resumo?r.textoResumo():null,null);
	}
}
