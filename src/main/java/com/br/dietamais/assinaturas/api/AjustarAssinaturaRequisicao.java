package com.br.dietamais.assinaturas.api;

import jakarta.validation.constraints.*;
import java.time.Instant;

/** Ajuste manual do admin (cortesia, correção, suporte). Só o que vier preenchido muda; o {@code motivo} é obrigatório e fica no log. */
public record AjustarAssinaturaRequisicao(
	@Size(max=40) String planoCodigo,
	StatusAssinatura status,
	Instant fimPeriodoEm,
	@NotBlank @Size(max=500) String motivo) { }
