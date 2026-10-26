package com.br.dietamais.pacientes.api;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroPacienteRequisicao(
		@NotBlank @Size(max=150) String nome,
		@NotBlank @Email @Size(max=255) String email,
		@NotBlank @Size(min = 10) String senha,
		@Schema(nullable = true) @Size(max=80) String fuso) {
	public CadastroPacienteRequisicao {
		nome=nome==null?null:nome.strip();email=email==null?null:email.strip().toLowerCase(java.util.Locale.ROOT);fuso=fuso==null?null:fuso.strip();
	}
	@Override public String toString(){return "CadastroPacienteRequisicao[credenciais protegidas]";}
}
