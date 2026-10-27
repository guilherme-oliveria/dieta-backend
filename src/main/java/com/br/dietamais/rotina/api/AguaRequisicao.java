package com.br.dietamais.rotina.api;

import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record AguaRequisicao(UUID id,@NotNull @Min(1) @Max(5000) Integer ml,@NotNull Instant registradoEm) {}
