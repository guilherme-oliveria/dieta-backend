package com.br.dietamais.contas.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.infra.ExclusaoContaStore;
import com.br.dietamais.contas.infra.SessaoStore;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exclusão da conta pelo próprio paciente (RN-LGPD-03). Exige a senha. Os dados pessoais são anonimizados e os vínculos encerrados;
 * o prontuário de cada nutri fica retido pelo prazo legal, sem a conta que o originou.
 */
@Service
public class ExcluirContaPaciente {
	private final UsuarioAtual usuario;private final SessaoStore contas;private final PasswordEncoder encoder;private final ExclusaoContaStore store;
	private final ApplicationEventPublisher eventos;private final Clock relogio;
	public ExcluirContaPaciente(UsuarioAtual usuario,SessaoStore contas,PasswordEncoder encoder,ExclusaoContaStore store,ApplicationEventPublisher eventos,Clock relogio){
		this.usuario=usuario;this.contas=contas;this.encoder=encoder;this.store=store;this.eventos=eventos;this.relogio=relogio;
	}
	@Transactional
	public void excluir(String senha){
		var contexto=usuario.obter();var paciente=contexto.exigirPaciente();var id=contexto.usuarioId();
		if(!contas.bloquearConta(id))throw new ExcecaoNegocio(CodigoErro.NAO_AUTENTICADO,null);
		var conta=contas.consultarConta(id);
		if(conta==null||!conta.ativo()||senha==null||!encoder.matches(senha,conta.senhaHash()))throw new ExcecaoNegocio(CodigoErro.CREDENCIAIS_INVALIDAS,"RN-CON-03");
		boolean inteira=!store.temPerfilNutri(id);
		eventos.publishEvent(new ContaPacienteExcluida(id,paciente,inteira));
		if(inteira)store.anonimizarConta(id,relogio.instant());else store.revogarSessoesPaciente(id);
	}
}
