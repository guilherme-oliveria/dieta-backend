package com.br.dietamais.avaliacoes.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Entradas complementares opcionais. Braço e dobras reutilizam as medidas da avaliação. */
public record DadosInfantisAvaliacao(@Min(1) @Max(20000) Integer pesoNascerGramas,ModoCurva modoCurva,EtniaSlaughter etniaSlaughter,
		@Positive @Digits(integer=1,fraction=3) BigDecimal fatorAtividade){}
