package com.br.dietamais.contas.api;

import jakarta.validation.constraints.*;
import java.util.UUID;

/** Cadastro profissional da nutri para o admin conferir o CRN. Nada de paciente (RN-LGPD-05). */
public record NutriAdminResposta(
	@NotNull UUID id,
	@NotBlank String nome,
	@NotBlank String email,
	@NotBlank String crn,
	@NotNull Boolean crnVerificado,
	@NotNull UUID organizacaoId) { }
