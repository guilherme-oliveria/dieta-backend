package com.br.dietamais.dietas.api;

import com.br.dietamais.alimentos.api.MarcadorAlimento;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record DietaSnapshotResposta(
	@NotNull UUID id,
	@NotNull UUID dietaId,
	@NotNull UUID pacienteId,
	@NotNull UUID vinculoId,
	@NotNull @Valid com.br.dietamais.pacientes.api.NutricionistaResumoResposta nutricionista,
	@NotBlank String titulo,
	String orientacao,
	@NotNull List<@NotNull @Valid RefeicaoResposta> refeicoes,
	@NotNull @Valid com.br.dietamais.alimentos.api.ListaSubstituicaoResposta listaSubstituicao,
	@NotNull @Valid TotaisNutricionaisResposta totais,
	@NotNull Instant publicadoEm,
	/** Congelados na publicação (RN-DIE-03, RN-DIE-10, RN-DIE-12); snapshots antigos não têm. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) List<@NotNull @Valid PorcaoGrupo> porcoes,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Valid @Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) MetasDieta metas,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) BigDecimal pesoReferenciaKg,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) String rodape,
	/** Restrições (marcadores de alimento) que a dieta evita; ausente = nenhuma (RN-MOD-03). */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) List<@NotNull MarcadorAlimento> restricoes) {
	public DietaSnapshotResposta(UUID id,UUID dietaId,UUID pacienteId,UUID vinculoId,com.br.dietamais.pacientes.api.NutricionistaResumoResposta nutricionista,String titulo,String orientacao,List<RefeicaoResposta> refeicoes,com.br.dietamais.alimentos.api.ListaSubstituicaoResposta listaSubstituicao,TotaisNutricionaisResposta totais,Instant publicadoEm,List<PorcaoGrupo> porcoes,MetasDieta metas,BigDecimal pesoReferenciaKg,String rodape){
		this(id,dietaId,pacienteId,vinculoId,nutricionista,titulo,orientacao,refeicoes,listaSubstituicao,totais,publicadoEm,porcoes,metas,pesoReferenciaKg,rodape,null);
	}
}
