package com.br.dietamais.contas.infra;

import com.br.dietamais.comum.api.*;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ContextoPacienteBanco implements ContextoPaciente {
	private final SessaoStore contas;
	public ContextoPacienteBanco(SessaoStore contas){this.contas=contas;}
	@Override public ContextoUsuario de(UUID usuarioId){
		var conta=contas.consultarConta(usuarioId);
		if(conta==null||!conta.ativo()||conta.pid()==null||!conta.perfis().contains(Perfil.PACIENTE))throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		return new ContextoUsuario(conta.resposta(Perfil.PACIENTE),null);
	}
}
