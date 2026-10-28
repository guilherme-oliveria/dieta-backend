package com.br.dietamais.notificacoes.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record AvisosResposta(
		@Valid @NotNull List<AvisoResposta> itens,
		@NotNull @PositiveOrZero Integer pagina,
		@NotNull @Min(1) @Max(100) Integer tamanho,
		@NotNull @PositiveOrZero Long total,
		@NotNull @PositiveOrZero Long naoLidos) {}
