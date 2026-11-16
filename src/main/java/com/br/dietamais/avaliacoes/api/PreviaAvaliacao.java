package com.br.dietamais.avaliacoes.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Resultado calculado de uma avaliação ainda não gravada (BE-55c): os mesmos números que a gravação congelaria, sem persistir nada. */
public record PreviaAvaliacao(
		@NotNull BigDecimal imc,
		@NotNull ClassificacaoImc classificacaoImc,
		@JsonInclude(JsonInclude.Include.NON_NULL) FaixaEtaria faixaEtaria,
		@JsonInclude(JsonInclude.Include.NON_NULL) @Valid ResultadosAvaliacao resultados,
		@JsonInclude(JsonInclude.Include.NON_NULL) @Valid EnergiaResultado energiaResultado,
		@JsonInclude(JsonInclude.Include.NON_NULL) @Valid ResultadosInfantisAvaliacao resultadosInfantis) {}
