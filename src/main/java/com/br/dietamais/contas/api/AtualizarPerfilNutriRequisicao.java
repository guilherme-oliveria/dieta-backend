package com.br.dietamais.contas.api;

import jakarta.validation.constraints.*;

public record AtualizarPerfilNutriRequisicao(
	@NotBlank @Size(max=150) String nome,
	@NotBlank @Size(max=20) @Pattern(regexp="CRN-[1-9][0-9]* [0-9]+") String crn,
	@Size(max=30) String telefone,
	@Size(max=10000) String orientacaoPadrao) {
	public AtualizarPerfilNutriRequisicao {
		nome=limpar(nome);crn=limpar(crn);telefone=limpar(telefone);orientacaoPadrao=limpar(orientacaoPadrao);
	}
	private static String limpar(String valor){return valor==null||valor.isBlank()?null:valor.strip();}
}
