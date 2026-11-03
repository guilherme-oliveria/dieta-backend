package com.br.dietamais.contas.api;

import com.br.dietamais.comum.api.Plataforma;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Nunca devolve o token FCM. */
public record DispositivoResposta(@NotNull UUID id,@NotNull Plataforma plataforma,@Schema(nullable=true) String nomeAparelho) {}
