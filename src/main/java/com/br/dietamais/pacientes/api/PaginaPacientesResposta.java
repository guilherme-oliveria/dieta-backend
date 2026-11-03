package com.br.dietamais.pacientes.api;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PaginaPacientesResposta(
		@Valid @NotNull List<PacienteResposta> itens,
		@NotNull @PositiveOrZero Integer pagina,
		@NotNull @Min(1) @Max(100) Integer tamanho,
		@NotNull @PositiveOrZero Long total) {
}
