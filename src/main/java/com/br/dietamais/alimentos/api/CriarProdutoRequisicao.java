package com.br.dietamais.alimentos.api;

import java.math.BigDecimal;
import java.util.*;
import jakarta.validation.constraints.*;

/** {@code id} é opcional (UUIDv7 gerado no app, offline-first). {@code nutrientes} são por 100 g (ou 100 mL) e precisam trazer energia, carboidrato, proteína e lipídeos. */
public record CriarProdutoRequisicao(
	UUID id,
	@NotBlank @Size(max=150) String nome,
	@Size(max=80) String marca,
	@Size(max=80) String classificacao,
	@Size(max=40) String apresentacao,
	@Size(max=200) String embalagem,
	@Size(max=400) String sabores,
	@Size(max=60) String medidaCaseira,
	@Positive @Digits(integer=8,fraction=4) BigDecimal medidaGramas,
	@Size(max=1200) String observacao,
	@NotNull Map<@NotBlank String,@NotNull BigDecimal> nutrientes) {
}
