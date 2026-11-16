package com.br.dietamais.referencias.api;

import java.math.BigDecimal;
import java.util.*;

/** Catálogo de exames e classificação do resultado (BE-56a). As linhas vêm do banco (`referencia_exame` e `referencia_exame_regra`). */
public interface ConsultaExames {
	/** Faixas etárias com catálogo: CRIANCA (0 a 9), ADOLESCENTE (10 a 17) e ADULTO (18 ou mais, idoso incluído). */
	List<String> faixasEtarias();
	List<ExameCatalogo> catalogo(String faixaEtaria);
	Optional<ExameCatalogo> exame(String faixaEtaria,String codigo);
	/**
	 * Classifica o resultado. Exame numérico usa {@code valor}; qualitativo usa {@code texto}. Vazio se o exame não existe na faixa.
	 * {@code sexo} é masculino ou feminino; {@code idadeAnos} é a idade na data do exame; {@code condicao} é a fase escolhida, entre as do exame.
	 */
	Optional<ClassificacaoExame> classificar(String faixaEtaria,String codigo,BigDecimal valor,String texto,String sexo,Double idadeAnos,String condicao);
	/**
	 * Faixas numéricas que valem para este paciente, na ordem da planilha (BE-56c). Regra de sexo, fase ou idade que não confere (ou cujo dado falta) fica de fora;
	 * exame qualitativo devolve lista vazia.
	 */
	List<FaixaExame> faixas(String faixaEtaria,String codigo,String sexo,Double idadeAnos,String condicao);
}
