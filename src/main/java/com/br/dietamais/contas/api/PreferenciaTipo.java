package com.br.dietamais.contas.api;

import jakarta.validation.constraints.NotNull;

public record PreferenciaTipo(@NotNull TipoNotificacao tipo,@NotNull Boolean ativo) {}
