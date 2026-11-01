package com.br.dietamais.dietas.api;

import jakarta.validation.constraints.NotBlank;

/** Aviso ao aplicar um modelo. Códigos: {@code ITEM_REMOVIDO_RESTRICAO}, {@code ITEM_INDISPONIVEL}, {@code SEM_ESCALA}. */
public record AvisoModelo(@NotBlank String codigo,@NotBlank String mensagem) {
}
