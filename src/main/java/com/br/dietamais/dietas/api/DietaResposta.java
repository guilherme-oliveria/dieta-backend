package com.br.dietamais.dietas.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record DietaResposta(
	@NotNull UUID id,
	@NotNull UUID pacienteId,
	@NotNull UUID vinculoId,
	@NotBlank String titulo,
	@NotNull StatusDieta status,
	String orientacao,
	@NotNull List<@NotNull @Valid RefeicaoResposta> refeicoes,
	@NotNull @Valid TotaisNutricionaisResposta totais,
	@NotNull @PositiveOrZero Long versao,
	@NotNull Instant criadoEm,
	@NotNull Instant atualizadoEm,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) List<@NotNull @Valid PorcaoGrupo> porcoes,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Valid @Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) MetasDieta metas,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) BigDecimal pesoReferenciaKg,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(nullable=true,requiredMode=Schema.RequiredMode.NOT_REQUIRED) String rodape) {
	public DietaResposta(UUID id,UUID pacienteId,UUID vinculoId,String titulo,StatusDieta status,String orientacao,List<RefeicaoResposta> refeicoes,TotaisNutricionaisResposta totais,Long versao,Instant criadoEm,Instant atualizadoEm){
		this(id,pacienteId,vinculoId,titulo,status,orientacao,refeicoes,totais,versao,criadoEm,atualizadoEm,null,null,null,null);
	}
}
