package com.br.dietamais.comum.api;

import java.util.List;

import tools.jackson.databind.JsonNode;

public class ExcecaoNegocio extends RuntimeException {
	private final CodigoErro codigo;
	private final String regra;
	private final List<ErroCampoResposta> campos;
	private final JsonNode atual;

	public ExcecaoNegocio(CodigoErro codigo, String regra) {
		this(codigo, regra, "A operação não atende às condições necessárias.", List.of(), null);
	}

	public ExcecaoNegocio(CodigoErro codigo, String regra, String mensagem,
			List<ErroCampoResposta> campos, JsonNode atual) {
		super(mensagem);
		if (regra != null && !codigo.regras().isEmpty() && !codigo.regras().contains(regra)) {
			throw new IllegalArgumentException("Regra incompatível com o código de erro");
		}
		this.codigo = codigo;
		this.regra = regra;
		this.campos = List.copyOf(campos);
		this.atual = atual;
	}

	public CodigoErro codigo() { return codigo; }
	public String regra() { return regra; }
	public List<ErroCampoResposta> campos() { return campos; }
	public JsonNode atual() { return atual; }
}
