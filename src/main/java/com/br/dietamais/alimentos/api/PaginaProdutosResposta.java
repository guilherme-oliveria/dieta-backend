package com.br.dietamais.alimentos.api;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record PaginaProdutosResposta(
	@NotNull List<@NotNull @Valid ProdutoResposta> itens,
	@PositiveOrZero int pagina,
	@Positive int tamanho,
	@PositiveOrZero long total) {
}
