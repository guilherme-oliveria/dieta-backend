package com.br.dietamais.comum.api;

public interface ManipuladorMutacao {
	/** Nome da entidade no envelope (ex.: {@code checkins}). */
	String entidade();
	/** Aplica a mutação no contexto do usuário; lança {@link ExcecaoNegocio} para rejeitar. */
	ResultadoMutacao aplicar(MutacaoSync mutacao,ContextoUsuario usuario);
}
