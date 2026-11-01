package com.br.dietamais.avaliacoes.api;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

/**
 * Resultado de exame lançado pela nutri na avaliação (BE-56b, RN-EXA-01). Exame numérico usa {@code valor}; qualitativo usa {@code texto}
 * (ausente, não reagente...). {@code condicao} é a fase do catálogo quando a referência depende dela (ciclo, menopausa, IMC...).
 */
public record ExameLancado(
		@NotBlank @Size(max=60) String codigo,
		@DecimalMin("0") @DecimalMax("1000000000") BigDecimal valor,
		@Size(max=100) String texto,
		@Size(max=100) String condicao) {
}
