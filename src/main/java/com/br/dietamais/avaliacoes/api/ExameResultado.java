package com.br.dietamais.avaliacoes.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;

/**
 * Exame gravado com a classificação calculada pelo servidor na hora do lançamento (BE-56b). {@code rotulo} é o texto da planilha; sem rótulo vem o
 * {@code motivo}: FALTA_SEXO, FALTA_CONDICAO, FALTA_IDADE ou FORA_DAS_FAIXAS (RN-CAL-04: nada de palpite).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExameResultado(
		@NotBlank String codigo,
		String secao,
		String nome,
		String unidade,
		BigDecimal valor,
		String texto,
		String condicao,
		String rotulo,
		String motivo,
		/** Faixas numéricas que valem para este paciente, para a régua do app (BE-56c); ausente em exame qualitativo. */
		java.util.List<com.br.dietamais.referencias.api.FaixaExame> faixas) {
	/** Contrato do BE-56b, sem faixas. */
	public ExameResultado(String codigo,String secao,String nome,String unidade,BigDecimal valor,String texto,String condicao,String rotulo,String motivo){
		this(codigo,secao,nome,unidade,valor,texto,condicao,rotulo,motivo,null);
	}
}
