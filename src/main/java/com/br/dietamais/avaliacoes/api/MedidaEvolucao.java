package com.br.dietamais.avaliacoes.api;

/** Séries da evolução. Todas vêm das avaliações, exceto {@code PESO_EM_CASA}, que vem dos registros do paciente. */
public enum MedidaEvolucao {
	PESO("kg"),IMC("kg/m²"),PERCENTUAL_GORDURA("%"),PESO_GORDO("kg"),PESO_MAGRO("kg"),
	PERIMETRO_PESCOCO("cm"),PERIMETRO_TORAX("cm"),PERIMETRO_CINTURA("cm"),PERIMETRO_ABDOMEN("cm"),PERIMETRO_QUADRIL("cm"),
	PERIMETRO_BRACO("cm"),PERIMETRO_ANTEBRACO("cm"),PERIMETRO_COXA("cm"),PERIMETRO_PANTURRILHA("cm"),PESO_EM_CASA("kg");
	private final String unidade;
	MedidaEvolucao(String unidade){this.unidade=unidade;}
	public String unidade(){return unidade;}
}
