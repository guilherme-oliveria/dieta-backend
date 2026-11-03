package com.br.dietamais.dietas.api;

import com.br.dietamais.alimentos.api.MarcadorAlimento;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record SalvarDietaRequisicao(
	UUID id,
	@NotBlank @Size(max=150) String titulo,
	@Size(max=10000) String orientacao,
	@NotNull List<@NotNull @Valid RefeicaoRequisicao> refeicoes,
	/** Porção escolhida por grupo, uma por dieta (RN-DIE-10). Opcional. */
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	@Size(max=40) List<@NotNull @Valid PorcaoGrupo> porcoes,
	/** Alvo da dieta. Opcional. */
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	@Valid MetasDieta metas,
	/** RN-DIE-12. Opcional. */
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	@Size(max=2000) String rodape,
	/** Marcadores de alimento que a dieta evita (RN-MOD-03). Ausente mantém os atuais; lista vazia limpa. */
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	@Size(max=5) List<@NotNull MarcadorAlimento> restricoes) {
	public SalvarDietaRequisicao(UUID id,String titulo,String orientacao,List<RefeicaoRequisicao> refeicoes,List<PorcaoGrupo> porcoes,MetasDieta metas,String rodape){this(id,titulo,orientacao,refeicoes,porcoes,metas,rodape,null);}
	public SalvarDietaRequisicao(UUID id,String titulo,String orientacao,List<RefeicaoRequisicao> refeicoes){this(id,titulo,orientacao,refeicoes,null,null,null,null);}
}
