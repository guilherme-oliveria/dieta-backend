package com.br.dietamais.dietas.api;

import jakarta.validation.constraints.*;
import java.util.UUID;

/** Porção escolhida para um grupo da dieta (RN-DIE-10): `opcao` é a de `GET /referencias/porcoes`. */
public record PorcaoGrupo(@NotNull UUID grupoId,@Positive int opcao) {
}
