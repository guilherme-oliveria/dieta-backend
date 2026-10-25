package com.br.dietamais.comum.aplicacao;

import java.util.List;
import java.util.Map;

public record RespostaIdempotente(int status, Map<String, List<String>> cabecalhos, byte[] corpo) {
	public RespostaIdempotente {
		cabecalhos = cabecalhos.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
				Map.Entry::getKey, entrada -> List.copyOf(entrada.getValue())));
		corpo = corpo.clone();
	}

	@Override
	public byte[] corpo() { return corpo.clone(); }
}
