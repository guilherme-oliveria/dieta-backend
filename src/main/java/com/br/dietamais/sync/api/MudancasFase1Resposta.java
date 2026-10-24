package com.br.dietamais.sync.api;

import java.util.List;
import com.br.dietamais.pacientes.api.NutricionistaResumoResposta;
import com.br.dietamais.pacientes.api.PacienteResposta;
import com.br.dietamais.pacientes.api.VinculoResposta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record MudancasFase1Resposta(
		@Valid @NotNull List<MudancaSyncResposta<PacienteResposta>> pacientes,
		@Valid @NotNull List<MudancaSyncResposta<VinculoResposta>> vinculos,
		@Valid @NotNull List<MudancaSyncResposta<NutricionistaResumoResposta>> nutris) {
}
