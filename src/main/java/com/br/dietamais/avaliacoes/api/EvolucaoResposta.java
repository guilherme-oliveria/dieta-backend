package com.br.dietamais.avaliacoes.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record EvolucaoResposta(@NotNull UUID pacienteId,@NotNull List<@NotNull @Valid SerieEvolucao> series) {}
