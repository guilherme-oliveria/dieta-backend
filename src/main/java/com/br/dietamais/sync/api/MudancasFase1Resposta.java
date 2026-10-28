package com.br.dietamais.sync.api;

import java.util.List;
import com.br.dietamais.alimentos.api.AlimentoResposta;
import com.br.dietamais.alimentos.api.GrupoAlimentoResposta;
import com.br.dietamais.alimentos.api.ListaSubstituicaoResposta;
import com.br.dietamais.avaliacoes.api.AvaliacaoResposta;
import com.br.dietamais.dietas.api.DietaEmUsoResposta;
import com.br.dietamais.dietas.api.DietaResposta;
import com.br.dietamais.dietas.api.DietaSnapshotResposta;
import com.br.dietamais.pacientes.api.NutricionistaResumoResposta;
import com.br.dietamais.questionarios.api.ModeloQuestionarioResposta;
import com.br.dietamais.questionarios.api.QuestionarioResposta;
import com.br.dietamais.pacientes.api.PacienteResposta;
import com.br.dietamais.pacientes.api.VinculoResposta;
import com.br.dietamais.rotina.api.AguaResposta;
import com.br.dietamais.rotina.api.CheckinResposta;
import com.br.dietamais.rotina.api.PesoResposta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Coleções do sync de leitura. As três primeiras são da fase 1; as demais entram na fase 2 (BE-14) como campos
 * opcionais: ausentes nas respostas antigas, sempre presentes (possivelmente vazias) nas novas.
 * Chaves do envelope: {@code listas} usa o id do vínculo, {@code dietaEmUso} o id do paciente e {@code grupos} o id do grupo.
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record MudancasFase1Resposta(
		@Valid @NotNull List<MudancaSyncResposta<PacienteResposta>> pacientes,
		@Valid @NotNull List<MudancaSyncResposta<VinculoResposta>> vinculos,
		@Valid @NotNull List<MudancaSyncResposta<NutricionistaResumoResposta>> nutris,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Valid List<MudancaSyncResposta<GrupoAlimentoResposta>> grupos,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Valid List<MudancaSyncResposta<AlimentoResposta>> alimentos,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Valid List<MudancaSyncResposta<ListaSubstituicaoResposta>> listas,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Valid List<MudancaSyncResposta<DietaResposta>> dietas,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Valid List<MudancaSyncResposta<DietaSnapshotResposta>> snapshots,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Valid List<MudancaSyncResposta<DietaEmUsoResposta>> dietaEmUso,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Valid List<MudancaSyncResposta<CheckinResposta>> checkins,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Valid List<MudancaSyncResposta<AguaResposta>> agua,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Valid List<MudancaSyncResposta<PesoResposta>> pesos,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Valid List<MudancaSyncResposta<AvaliacaoResposta>> avaliacoes,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Valid List<MudancaSyncResposta<ModeloQuestionarioResposta>> modelos,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Valid List<MudancaSyncResposta<QuestionarioResposta>> questionarios) {
	public MudancasFase1Resposta(List<MudancaSyncResposta<PacienteResposta>> pacientes,List<MudancaSyncResposta<VinculoResposta>> vinculos,List<MudancaSyncResposta<NutricionistaResumoResposta>> nutris) {
		this(pacientes,vinculos,nutris,null,null,null,null,null,null,null,null,null,null,null,null);
	}
}
