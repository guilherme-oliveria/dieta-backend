package com.br.dietamais.assinaturas.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CriarPlanoRequisicao(
	@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{1,39}") String codigo,
	@NotBlank @Size(max=100) String nome,
	@NotNull @PositiveOrZero @Digits(integer=8,fraction=2) BigDecimal precoMensal,
	@NotNull @Min(1) @Max(100000) Integer limitePacientes,
	@Min(1) @Max(3650) Integer duracaoDias,
	@NotBlank @Size(max=500) String motivo) { }
