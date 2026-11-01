package com.br.dietamais.referencias.api;

import java.util.Optional;

/** Consulta às curvas da OMS (BE-54). Só leitura; as linhas vêm do banco (`referencia_oms`). */
public interface ConsultaOms {
	/**
	 * Linha para o indicador, o sexo ({@code masculino} ou {@code feminino}) e a idade em meses. Peso e estatura por idade vão de 0 a 119 meses;
	 * o IMC por idade vai de 0 a 119 meses (mensal) e de 10 a 17 anos (linha do ano completo, até 215 meses). Fora disso devolve vazio (RN-CAL-04: nada de zero).
	 */
	Optional<CurvaOms> curva(String indicador,String sexo,int idadeMeses);
}
