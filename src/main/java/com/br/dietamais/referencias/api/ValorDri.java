package com.br.dietamais.referencias.api;

import java.math.BigDecimal;

/** Ingestão de referência de um nutriente para um perfil (BE-45). {@code nutriente} é o código estável; potássio e sódio em mg (correção 8 da seção 3.6) e vitamina A em mcg (correção 9). */
public record ValorDri(String nutriente,String nome,String unidade,BigDecimal valor){}
