package com.br.dietamais.rotina.api;

import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record AguaResposta(@NotNull UUID id,@NotNull UUID pacienteId,@NotNull @Positive Integer ml,@NotNull Instant registradoEm) {}
