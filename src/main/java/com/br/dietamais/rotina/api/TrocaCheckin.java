package com.br.dietamais.rotina.api;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

/** Item da refeição trocado por outro alimento do mesmo grupo da lista congelada (RN-SUB-01/03). */
public record TrocaCheckin(@NotNull UUID itemId,@NotNull UUID alimentoId) {}
