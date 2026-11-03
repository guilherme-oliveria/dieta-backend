package com.br.dietamais.alimentos.api;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record GruposAlimentosResposta(
	@NotNull List<@NotNull @Valid GrupoAlimentoResposta> itens) {
}
