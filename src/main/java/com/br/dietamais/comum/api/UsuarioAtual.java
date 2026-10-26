package com.br.dietamais.comum.api;

public interface UsuarioAtual {
	ContextoUsuario obter();
	default java.util.Optional<ContextoUsuario> opcional(){return java.util.Optional.of(obter());}
}
