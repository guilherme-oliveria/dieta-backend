package com.br.dietamais.questionarios.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code padrao}: cópia do modelo padrão que toda nutri recebe (RN-ANA-01); ela pode editá-la como qualquer outro modelo. */
public record ModeloQuestionarioResposta(@NotNull UUID id,@NotNull String nome,@NotNull Boolean padrao,@NotNull List<@NotNull @Valid BlocoQuestionario> blocos,
		@NotNull @PositiveOrZero Long versao,@NotNull Instant criadoEm,@NotNull Instant atualizadoEm) {}
