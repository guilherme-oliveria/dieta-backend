package com.br.dietamais.contas.api;

import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record PerfilNutriResposta(
	@NotNull UUID id,
	@NotBlank String nome,
	@NotBlank String crn,
	String telefone,
	String orientacaoPadrao,
	Boolean temLogo,
	@NotNull @PositiveOrZero Long versao,
	@NotNull Instant atualizadoEm) {
}
