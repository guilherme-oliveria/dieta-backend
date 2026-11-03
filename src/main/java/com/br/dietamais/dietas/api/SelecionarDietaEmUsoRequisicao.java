package com.br.dietamais.dietas.api;

import java.util.UUID;
import jakarta.validation.constraints.*;

public record SelecionarDietaEmUsoRequisicao(
	@NotNull UUID dietaId) {
}
