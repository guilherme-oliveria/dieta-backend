package com.br.dietamais.dietas.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.*;

/** Aplica um modelo a um paciente (RN-MOD-02). {@code vetKcal} é o alvo do paciente; sem ele as porções ficam como no modelo. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AplicarModeloRequisicao(
	/** Id (UUIDv7) do rascunho a criar; opcional. */
	UUID id,
	@Positive @DecimalMax("20000") BigDecimal vetKcal) {
}
