package com.br.dietamais.pacientes.api;

import io.swagger.v3.oas.annotations.media.Schema;

import com.br.dietamais.contas.api.DispositivoRequisicao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AceitarConviteRequisicao(
		@NotNull @AssertTrue Boolean aceitouTermo,
		@NotBlank String versaoTermo,
		@Schema(nullable = true) @Valid CadastroPacienteRequisicao cadastro,
		@Valid @NotNull DispositivoRequisicao dispositivo) {
}
