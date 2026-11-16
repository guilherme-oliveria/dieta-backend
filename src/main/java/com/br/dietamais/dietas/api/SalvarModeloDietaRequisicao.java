package com.br.dietamais.dietas.api;

import com.br.dietamais.alimentos.api.MarcadorAlimento;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/** Cria ou substitui um modelo da nutri (BE-52). O conteúdo segue as mesmas regras da dieta (RN-DIE-10, RN-DIE-11). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SalvarModeloDietaRequisicao(
	UUID id,
	@NotBlank @Size(max=150) String titulo,
	@Size(max=500) String descricao,
	/** Condição a que o modelo se destina (ex.: Hipertensão); texto livre curto. */
	@Size(max=60) String condicao,
	@Size(max=10000) String orientacao,
	@NotNull List<@NotNull @Valid RefeicaoRequisicao> refeicoes,
	@Size(max=40) List<@NotNull @Valid PorcaoGrupo> porcoes,
	@Valid MetasDieta metas,
	@Size(max=2000) String rodape,
	/** Marcadores de alimento que o modelo evita (RN-MOD-03). */
	@Size(max=5) List<@NotNull MarcadorAlimento> restricoes,
	/** Energia total do modelo em kcal; base para escalar as porções ao aplicar (RN-MOD-02). Sem ela, usa {@code metas.vetKcal}. */
	@Positive @DecimalMax("20000") BigDecimal kcalReferencia) {
}
