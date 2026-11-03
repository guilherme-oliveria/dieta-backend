package com.br.dietamais.dietas.api;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record DietasResposta(
	@NotNull List<@NotNull @Valid DietaResposta> itens) {
}
