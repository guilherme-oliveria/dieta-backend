package com.br.dietamais.questionarios.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SalvarModeloRequisicao(UUID id,@NotBlank @Size(max=120) String nome,@NotEmpty @Size(max=30) List<@Valid BlocoQuestionario> blocos) {}
