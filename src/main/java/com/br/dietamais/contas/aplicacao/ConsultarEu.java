package com.br.dietamais.contas.aplicacao;

import com.br.dietamais.assinaturas.api.ConsultarAssinatura;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.api.EuResposta;
import com.br.dietamais.pacientes.api.ConsultarVinculos;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConsultarEu {
	private final UsuarioAtual usuario;
	private final ConsultarAssinatura assinaturas;
	private final ConsultarVinculos vinculos;
	public ConsultarEu(UsuarioAtual usuario,ConsultarAssinatura assinaturas,ConsultarVinculos vinculos){this.usuario=usuario;this.assinaturas=assinaturas;this.vinculos=vinculos;}
	public EuResposta executar() {
		var contexto=usuario.obter();var perfil=contexto.perfil();
		return new EuResposta(contexto.usuario(),perfil==Perfil.NUTRICIONISTA?assinaturas.atual():null,
				perfil==Perfil.PACIENTE?vinculos.atuais():List.of());
	}
}
