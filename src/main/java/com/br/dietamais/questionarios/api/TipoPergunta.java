package com.br.dietamais.questionarios.api;

/** RN-ANA-02. Formato do valor de resposta: TEXTO string; NUMERO número; ESCOLHA_UNICA string de {@code opcoes}; MULTIPLA_ESCOLHA lista de strings de {@code opcoes};
 *  ESCALA_1_5 inteiro de 1 a 5; HORARIO "HH:mm"; LISTA_POR_REFEICAO lista de {@code {refeicao, descricao, horario?}}. */
public enum TipoPergunta { TEXTO, NUMERO, ESCOLHA_UNICA, MULTIPLA_ESCOLHA, ESCALA_1_5, HORARIO, LISTA_POR_REFEICAO }
