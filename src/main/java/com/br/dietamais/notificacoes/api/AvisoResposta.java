package com.br.dietamais.notificacoes.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

/** Texto genérico (RN-NOT-02). {@code tipo} é um {@link TipoAviso}; clientes tratam valor desconhecido como "outro". */
public record AvisoResposta(
		@NotNull UUID id,
		@NotNull @Schema(description="NOVA_DIETA, QUESTIONARIO_ENVIADO, CONSULTA_AMANHA, QUESTIONARIO_RESPONDIDO, PACIENTE_SEM_CHECKIN, ASSINATURA (podem surgir outros)") String tipo,
		@NotBlank @Size(max=80) String titulo,
		@NotBlank @Size(max=200) String corpo,
		@Schema(description="Destino da navegação no app, ex.: /dieta, /questionarios/{id}, /pacientes/{id}, /agenda",nullable=true) String rota,
		@NotNull Instant criadoEm,
		@Schema(description="Nulo enquanto não lido",nullable=true) Instant lidoEm,
		@NotNull @PositiveOrZero Long versao) {}
