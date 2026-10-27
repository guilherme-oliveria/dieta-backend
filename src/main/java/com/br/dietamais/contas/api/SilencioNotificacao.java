package com.br.dietamais.contas.api;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/** Janela sem push; pode cruzar a meia-noite (22:00 a 07:00), no fuso do usuário. */
public record SilencioNotificacao(@NotNull Boolean ativo,@NotNull LocalTime inicio,@NotNull LocalTime fim) {}
