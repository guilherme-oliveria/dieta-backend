package com.br.dietamais.rotina.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AdesaoResposta(@NotNull UUID pacienteId,@NotNull @Valid JanelaAdesao dias7,@NotNull @Valid JanelaAdesao dias30) {
}
