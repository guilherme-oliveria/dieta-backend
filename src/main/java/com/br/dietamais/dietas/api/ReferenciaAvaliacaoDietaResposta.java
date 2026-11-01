package com.br.dietamais.dietas.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * O que a avaliação mais recente do paciente oferece à dieta (BE-49d): peso dos "por kg" (RN-CAL-05, ajustado com IMC 30 ou mais), metas de
 * energia e macros para preencher o planejador e a condição de gestante ou lactante usada na adequação DRI. {@code metas} é ausente quando a
 * avaliação não calculou o VET. O cliente só mostra e sugere; quem grava e confere é o servidor.
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ReferenciaAvaliacaoDietaResposta(UUID avaliacaoId,Instant dataHora,BigDecimal pesoKg,BigDecimal pesoReferenciaKg,MetasDieta metas,boolean gestante,boolean lactante) {
}
