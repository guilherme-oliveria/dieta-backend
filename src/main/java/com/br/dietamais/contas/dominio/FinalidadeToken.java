package com.br.dietamais.contas.dominio;

public enum FinalidadeToken {
	CONFIRMAR_EMAIL("RN-CON-05"), REDEFINIR_SENHA("RN-CON-07");
	private final String regra;
	FinalidadeToken(String regra) { this.regra = regra; }
	public String regra() { return regra; }
}
