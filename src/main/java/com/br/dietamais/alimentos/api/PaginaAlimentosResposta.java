package com.br.dietamais.alimentos.api;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record PaginaAlimentosResposta(
	@NotNull List<@NotNull @Valid AlimentoResposta> itens,
	@PositiveOrZero int pagina,
	@Positive int tamanho,
	@PositiveOrZero long total) {
}
