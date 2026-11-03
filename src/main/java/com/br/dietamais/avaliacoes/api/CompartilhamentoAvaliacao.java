package com.br.dietamais.avaliacoes.api;

/**
 * RN-RES-01: o que o paciente vê, por seção. {@code null} em uma chave = padrão (tudo {@code true}, exceto
 * {@code exames}, que é {@code false}). A chave antiga {@code compartilhada} continua mandando: {@code false} esconde tudo.
 */
public record CompartilhamentoAvaliacao(Boolean medidas,Boolean composicao,Boolean necessidades,Boolean resumo,Boolean exames) {
}
