package com.br.dietamais.referencias.api;

import java.math.BigDecimal;
import java.util.Optional;

/** Consulta às tabelas de referência clínica (BE-39). Só leitura; as linhas vêm do banco (`referencia_faixa`). */
public interface ConsultaReferencias {
	/**
	 * Código da faixa de {@code tabela} que contém {@code valor} para o sexo e a idade informados.
	 * {@code sexo} ({@code "masculino"} ou {@code "feminino"}) e {@code idadeAnos} podem ser nulos quando a tabela não depende deles.
	 * Faixa de idade é [min, max); faixa de valor respeita os marcadores de limite inclusivo.
	 */
	Optional<String> classificar(String tabela,String sexo,Integer idadeAnos,BigDecimal valor);

	/** Versão das tabelas de referência em vigor (gravada em cada avaliação, RN-CAL-03). */
	String versaoVigente();
}
