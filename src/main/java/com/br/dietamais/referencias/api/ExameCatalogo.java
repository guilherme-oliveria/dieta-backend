package com.br.dietamais.referencias.api;

import java.util.List;

/** Exame do catálogo de uma faixa etária (BE-56a). {@code condicoes} lista as opções que a nutri escolhe quando a referência depende de fase (ciclo, menopausa, IMC...); vazio se não depende. */
public record ExameCatalogo(String codigo,String secao,String nome,String unidade,boolean qualitativo,List<String> condicoes){}
