package com.br.dietamais.contas.api;

import jakarta.validation.constraints.*;

public record VerificarCrnRequisicao(@NotNull Boolean verificado,@NotBlank @Size(max=500) String motivo) { }
