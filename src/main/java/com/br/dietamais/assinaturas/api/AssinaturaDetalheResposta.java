package com.br.dietamais.assinaturas.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Tela de assinatura da nutri (BE-26). {@code carenciaAte} só vem em {@code EM_ATRASO}: depois dela a nutri fica só leitura (RN-ASS-04).
 * {@code planosDisponiveis} são os pagos; a contratação acontece no checkout hospedado (RN-ASS-06).
 */
public record AssinaturaDetalheResposta(
		@NotNull UUID organizacaoId,
		@NotNull PlanoResposta plano,
		@NotNull StatusAssinatura status,
		@NotNull @PositiveOrZero Integer pacientesOcupados,
		@Schema(nullable=true) Instant fimPeriodoEm,
		@Schema(nullable=true) Instant carenciaAte,
		@NotNull Boolean pagamentoDisponivel,
		@NotNull List<PlanoResposta> planosDisponiveis) { }
