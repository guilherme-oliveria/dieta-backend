package com.br.dietamais.referencias.api;

import java.math.BigDecimal;

/**
 * Um degrau da classificação de um exame, na ordem da planilha (o primeiro que serve vale, como nos IF aninhados). Campos nulos não restringem.
 * Idade em anos (na data do exame); valor no limite pode ser incluído ou não. {@code textoIgual} é para exames qualitativos (ausente, não reagente...).
 */
public record RegraExame(int ordem,String sexo,String condicao,BigDecimal idadeMin,boolean idadeMinIncl,BigDecimal idadeMax,boolean idadeMaxIncl,
		BigDecimal valorMin,boolean valorMinIncl,BigDecimal valorMax,boolean valorMaxIncl,String textoIgual,String rotulo){
	/** Motivo de não ter encontrado degrau: dado que a regra pedia e não veio, ou o valor ficou fora de todas as faixas. */
	enum Falta{NENHUMA,SEXO,CONDICAO,IDADE,VALOR}
	Falta confere(String sexoPaciente,Double idadeAnos,String condicaoEscolhida,BigDecimal valor,String texto){
		if(sexo!=null){if(sexoPaciente==null)return Falta.SEXO;if(!sexo.equals(sexoPaciente))return Falta.VALOR;}
		if(condicao!=null){if(condicaoEscolhida==null)return Falta.CONDICAO;if(!condicao.equals(condicaoEscolhida))return Falta.VALOR;}
		if(idadeMin!=null||idadeMax!=null){
			if(idadeAnos==null)return Falta.IDADE;
			var i=BigDecimal.valueOf(idadeAnos);
			if(!dentro(i,idadeMin,idadeMinIncl,idadeMax,idadeMaxIncl))return Falta.VALOR;
		}
		if(textoIgual!=null)return textoIgual.equals(texto)?Falta.NENHUMA:Falta.VALOR;
		if(valorMin!=null||valorMax!=null){
			if(valor==null)return Falta.VALOR;
			if(!dentro(valor,valorMin,valorMinIncl,valorMax,valorMaxIncl))return Falta.VALOR;
		}
		return Falta.NENHUMA;
	}
	/** Faixa numérica desta regra se ela vale para este paciente; vazia se pede sexo, fase ou idade que não bate (ou que falta) ou se é qualitativa. */
	java.util.Optional<FaixaExame> faixaPara(String sexoPaciente,Double idadeAnos,String condicaoEscolhida){
		if(textoIgual!=null||valorMin==null&&valorMax==null)return java.util.Optional.empty();
		if(sexo!=null&&!sexo.equals(sexoPaciente)||condicao!=null&&!condicao.equals(condicaoEscolhida))return java.util.Optional.empty();
		if(idadeMin!=null||idadeMax!=null){if(idadeAnos==null||!dentro(BigDecimal.valueOf(idadeAnos),idadeMin,idadeMinIncl,idadeMax,idadeMaxIncl))return java.util.Optional.empty();}
		return java.util.Optional.of(new FaixaExame(rotulo,valorMin,valorMinIncl,valorMax,valorMaxIncl));
	}
	private static boolean dentro(BigDecimal v,BigDecimal min,boolean minIncl,BigDecimal max,boolean maxIncl){
		if(min!=null){int c=v.compareTo(min);if(c<0||(c==0&&!minIncl))return false;}
		if(max!=null){int c=v.compareTo(max);if(c>0||(c==0&&!maxIncl))return false;}
		return true;
	}
}
