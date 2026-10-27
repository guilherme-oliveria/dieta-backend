package com.br.dietamais.alimentos.api;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record GrupoAlimentoResposta(
	@NotNull UUID id,
	@NotBlank String nome,
	@NotNull @PositiveOrZero BigDecimal kcalPorPorcao,
	@NotNull @PositiveOrZero BigDecimal carboidratosPorPorcao,
	@NotNull @PositiveOrZero BigDecimal proteinasPorPorcao,
	@NotNull @PositiveOrZero BigDecimal gordurasPorPorcao,
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	@io.swagger.v3.oas.annotations.media.Schema(nullable=true,requiredMode=io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED,description="Indica macros iniciais estimados por representantes; ausente em contratos anteriores") Boolean macrosEstimados,
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	@io.swagger.v3.oas.annotations.media.Schema(nullable=true,requiredMode=io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED)
	@PositiveOrZero Long versao,
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	@io.swagger.v3.oas.annotations.media.Schema(nullable=true,requiredMode=io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED)
	OrigemMacros origemMacros) {
	public GrupoAlimentoResposta(UUID id,String nome,BigDecimal kcal,BigDecimal carboidratos,BigDecimal proteinas,BigDecimal gorduras,Boolean estimados){this(id,nome,kcal,carboidratos,proteinas,gorduras,estimados,null,null);}
}
