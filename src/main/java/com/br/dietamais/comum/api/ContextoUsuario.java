package com.br.dietamais.comum.api;

import java.util.Objects;
import java.util.UUID;

public record ContextoUsuario(UsuarioResposta usuario, UUID organizacaoId) {
	public ContextoUsuario { Objects.requireNonNull(usuario);Objects.requireNonNull(usuario.perfil()); }
	public UUID usuarioId(){return usuario.id();}
	public Perfil perfil(){return usuario.perfil();}
	public UUID exigirNutricionista() {
		if(perfil()!=Perfil.NUTRICIONISTA||usuario.nutricionistaId()==null||organizacaoId==null)throw new ExcecaoNegocio(CodigoErro.PERFIL_INVALIDO,null);
		return usuario.nutricionistaId();
	}
	public UUID exigirPaciente() {
		if(perfil()!=Perfil.PACIENTE||usuario.pacienteId()==null)throw new ExcecaoNegocio(CodigoErro.PERFIL_INVALIDO,null);
		return usuario.pacienteId();
	}
	@Override public String toString(){return "ContextoUsuario[identidade protegida]";}
}
