package com.br.dietamais.alimentos.dominio;

import com.br.dietamais.comum.api.*;
import java.util.*;

public record DadosAjusteLista(List<UUID> adicionados,List<UUID> removidos){
	public DadosAjusteLista {
		if(adicionados==null||removidos==null||adicionados.stream().anyMatch(Objects::isNull)||removidos.stream().anyMatch(Objects::isNull)
			||new HashSet<>(adicionados).size()!=adicionados.size()||new HashSet<>(removidos).size()!=removidos.size()
			||!Collections.disjoint(adicionados,removidos))throw new ExcecaoNegocio(CodigoErro.FORMATO_INVALIDO,"RN-ALI-01");
		adicionados=adicionados.stream().sorted(Comparator.comparing(UUID::toString)).toList();removidos=removidos.stream().sorted(Comparator.comparing(UUID::toString)).toList();
	}
}
