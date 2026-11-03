package com.br.dietamais.relatorios.api;

/** Carboidratos usa a porção média e inclui a contagem por porção. */
public enum TamanhoListaPdf {
	PEQUENA("Porção pequena"), MEDIA("Porção média"), GRANDE("Porção grande"), CARBOIDRATOS("Contagem de carboidratos");
	private final String rotulo;
	TamanhoListaPdf(String rotulo){this.rotulo=rotulo;}
	public String rotulo(){return rotulo;}
}
