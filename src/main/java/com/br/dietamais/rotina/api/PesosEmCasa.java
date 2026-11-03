package com.br.dietamais.rotina.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Leitura dos pesos que o paciente registra em casa (RN-AVA-03), para outros módulos como a evolução. */
public interface PesosEmCasa {
	record Ponto(Instant registradoEm,BigDecimal pesoKg) {}
	/** Os {@code limite} registros mais recentes, em ordem cronológica crescente. */
	List<Ponto> doPaciente(UUID paciente,int limite);
}
