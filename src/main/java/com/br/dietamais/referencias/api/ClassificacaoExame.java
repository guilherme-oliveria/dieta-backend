package com.br.dietamais.referencias.api;

/**
 * Resultado da classificação de um exame (BE-56a). {@code rotulo} é o texto da planilha ("normal", "alto", "limite superior"...);
 * sem rótulo vem o {@code motivo}: FALTA_SEXO, FALTA_CONDICAO, FALTA_IDADE ou FORA_DAS_FAIXAS (RN-CAL-04: nada de zero nem de palpite).
 */
public record ClassificacaoExame(String rotulo,String motivo){
	public static ClassificacaoExame de(String rotulo){return new ClassificacaoExame(rotulo,null);}
	public static ClassificacaoExame sem(String motivo){return new ClassificacaoExame(null,motivo);}
}
