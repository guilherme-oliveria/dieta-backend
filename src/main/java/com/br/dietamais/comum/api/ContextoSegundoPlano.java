package com.br.dietamais.comum.api;

import java.util.Optional;
import java.util.function.Supplier;

/** Quem age em trabalho de segundo plano (sem requisição), para a auditoria saber o titular, p.ex. na exportação de dados. */
public final class ContextoSegundoPlano {
	private static final ThreadLocal<ContextoUsuario> ATUAL=new ThreadLocal<>();
	private ContextoSegundoPlano() { }
	public static <T> T como(ContextoUsuario contexto,Supplier<T> acao){
		var anterior=ATUAL.get();ATUAL.set(contexto);
		try{return acao.get();}finally{if(anterior==null)ATUAL.remove();else ATUAL.set(anterior);}
	}
	public static Optional<ContextoUsuario> atual(){return Optional.ofNullable(ATUAL.get());}
}
