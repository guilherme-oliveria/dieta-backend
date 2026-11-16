package com.br.dietamais.dietas.api;

import com.br.dietamais.alimentos.api.MarcadorAlimento;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Modelo de dieta (BE-52). {@code escopo} é {@code SISTEMA} (somente leitura, RN-MOD-01) ou {@code NUTRI} (cópia da nutri que consulta).
 * O conteúdo tem a mesma forma da requisição da dieta; os itens por alimento valem para a lista da nutri que salvou.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModeloDietaResposta(
	@NotNull UUID id,
	@NotBlank @Pattern(regexp="SISTEMA|NUTRI") String escopo,
	@NotBlank String titulo,
	String descricao,
	String condicao,
	String orientacao,
	@NotNull List<@NotNull @Valid RefeicaoRequisicao> refeicoes,
	List<@NotNull @Valid PorcaoGrupo> porcoes,
	@Valid MetasDieta metas,
	String rodape,
	@NotNull List<@NotNull MarcadorAlimento> restricoes,
	BigDecimal kcalReferencia,
	@NotNull @PositiveOrZero Long versao,
	@NotNull Instant criadoEm,
	@NotNull Instant atualizadoEm) {
}
