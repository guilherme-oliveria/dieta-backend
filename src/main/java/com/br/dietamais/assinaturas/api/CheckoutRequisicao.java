package com.br.dietamais.assinaturas.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckoutRequisicao(@NotBlank @Size(max=40) String plano) { }
