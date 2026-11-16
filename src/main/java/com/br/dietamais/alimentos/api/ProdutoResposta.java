package com.br.dietamais.alimentos.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import jakarta.validation.constraints.*;

/** Produto ou suplemento da nutri (BE-48b, RN-ALI-05). {@code nutrientes} por 100 g (ou 100 mL), mesmas chaves de {@code /referencias/produtos}. */
public record ProdutoResposta(
	@NotNull UUID id,
	@NotBlank String nome,
	String marca,
	String classificacao,
	String apresentacao,
	String embalagem,
	String sabores,
	String medidaCaseira,
	BigDecimal medidaGramas,
	String observacao,
	@NotNull Map<String,BigDecimal> nutrientes,
	boolean ativo,
	@NotNull @PositiveOrZero Long versao,
	@NotNull Instant atualizadoEm) {
}
