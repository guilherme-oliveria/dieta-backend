package com.br.dietamais.rotina.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record PesoRequisicao(UUID id,@NotNull BigDecimal pesoKg,@NotNull Instant registradoEm) {}
