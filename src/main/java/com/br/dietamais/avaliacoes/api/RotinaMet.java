package com.br.dietamais.avaliacoes.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/** Rotina do dia para o GET por MET (aba MET): sono, trabalho (intensidade leve, moderada ou intensa) e até 5 atividades. */
public record RotinaMet(@NotNull BigDecimal horasSono,@NotNull BigDecimal horasTrabalho,@NotNull NivelAtividade intensidadeTrabalho,
		@Size(max=5) List<@Valid AtividadeDiaria> atividades) {
}
