package com.br.dietamais.assinaturas.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** O código do plano não muda. Preço novo vale para novos checkouts; quem já assina continua no valor do gateway. */
public record AtualizarPlanoRequisicao(
	@NotBlank @Size(max=100) String nome,
	@NotNull @PositiveOrZero @Digits(integer=8,fraction=2) BigDecimal precoMensal,
	@NotNull @Min(1) @Max(100000) Integer limitePacientes,
	@Min(1) @Max(3650) Integer duracaoDias,
	@NotNull Boolean ativo,
	@NotBlank @Size(max=500) String motivo) { }
