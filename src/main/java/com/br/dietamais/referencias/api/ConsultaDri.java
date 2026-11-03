package com.br.dietamais.referencias.api;

import java.util.*;

/** Consulta às ingestões de referência (DRI 2005, BE-45). Só leitura; as linhas vêm do banco (`referencia_dri`). */
public interface ConsultaDri {
	/**
	 * Metas de todos os nutrientes que têm referência para o perfil, na ordem da tabela. Lactante tem prioridade sobre gestante.
	 * Nutriente sem referência para a idade (ex.: fibras até 1 ano) não aparece (RN-CAL-04: nada de zero).
	 */
	List<ValorDri> metas(String sexo,int idadeMeses,boolean gestante,boolean lactante);
	Optional<ValorDri> meta(String nutriente,String sexo,int idadeMeses,boolean gestante,boolean lactante);
}
