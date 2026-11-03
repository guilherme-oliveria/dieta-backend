package com.br.dietamais.dietas.api;

import java.util.UUID;
import jakarta.validation.constraints.*;

public record DietaEmUsoResposta(
	@NotNull UUID dietaId,
	@NotNull @PositiveOrZero Long versao) {
}
