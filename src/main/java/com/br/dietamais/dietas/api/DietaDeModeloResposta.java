package com.br.dietamais.dietas.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Rascunho criado a partir de um modelo, o fator de escala aplicado às porções e o que foi deixado de fora (RN-MOD-02, RN-MOD-03). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DietaDeModeloResposta(
	@NotNull @Valid DietaResposta dieta,
	@NotNull BigDecimal fatorEscala,
	@NotNull List<@NotNull @Valid AvisoModelo> avisos) {
}
