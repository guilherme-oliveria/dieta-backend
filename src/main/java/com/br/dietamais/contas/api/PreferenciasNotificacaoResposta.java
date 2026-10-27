package com.br.dietamais.contas.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

/** Tipos do perfil ativo; {@code versao} 0 = ainda não personalizado (padrão: tudo ligado, silêncio 22:00 a 07:00). */
public record PreferenciasNotificacaoResposta(@NotNull List<@NotNull @Valid PreferenciaTipo> tipos,@NotNull @Valid SilencioNotificacao silencio,@NotNull @PositiveOrZero Long versao) {}
