package com.br.dietamais.contas.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExcluirContaRequisicao(@NotBlank @Size(max=200) String senha) {
	@Override public String toString(){return "ExcluirContaRequisicao[dados protegidos]";}
}
