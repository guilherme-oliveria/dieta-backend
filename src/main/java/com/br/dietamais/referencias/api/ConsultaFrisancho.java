package com.br.dietamais.referencias.api;

import java.util.Optional;

/** Consulta aos percentis de Frisancho de 2 a 9 anos (BE-54b). Fora dessa idade devolve vazio (RN-CAL-04). */
public interface ConsultaFrisancho {
	Optional<PercentisFrisancho> percentis(String indicador,String sexo,int idadeAnos);
}
