package com.br.dietamais.rotina.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record PesoResposta(@NotNull UUID id,@NotNull UUID pacienteId,@NotNull @Positive BigDecimal pesoKg,@NotNull Instant registradoEm) {}
