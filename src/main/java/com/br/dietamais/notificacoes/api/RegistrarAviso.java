package com.br.dietamais.notificacoes.api;

/** Grava o aviso no histórico do usuário e o põe na fila de push. Falhas aqui nunca derrubam quem chama. */
public interface RegistrarAviso { void registrar(NovoAviso aviso); }
