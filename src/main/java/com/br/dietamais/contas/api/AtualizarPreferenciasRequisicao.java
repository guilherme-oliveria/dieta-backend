package com.br.dietamais.contas.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AtualizarPreferenciasRequisicao(@NotNull @Size(max=20) List<@NotNull @Valid PreferenciaTipo> tipos,@NotNull @Valid SilencioNotificacao silencio) {}
