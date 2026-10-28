package com.br.dietamais.avaliacoes.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Pontos em ordem cronológica crescente; séries sem nenhum ponto não aparecem. */
public record SerieEvolucao(@NotNull MedidaEvolucao medida,@NotNull String unidade,@NotNull List<@NotNull @Valid PontoEvolucao> pontos) {}
