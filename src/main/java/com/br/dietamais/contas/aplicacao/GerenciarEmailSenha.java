package com.br.dietamais.contas.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.dominio.*;
import com.br.dietamais.contas.infra.*;
import java.time.Clock;
import org.slf4j.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GerenciarEmailSenha {
	private final SessaoStore contas;
	private final TokenContaStore tokens;
	private final EmitirTokenConta emissor;
	private final PoliticaSenha politica;
	private final PasswordEncoder encoder;
	private final Clock relogio;
	private final TransactionTemplate transacao;
	public GerenciarEmailSenha(SessaoStore contas,TokenContaStore tokens,EmitirTokenConta emissor,PoliticaSenha politica,
			PasswordEncoder encoder,Clock relogio,PlatformTransactionManager transacoes) {
		this.contas=contas;this.tokens=tokens;this.emissor=emissor;this.politica=politica;
		this.encoder=encoder;this.relogio=relogio;this.transacao=new TransactionTemplate(transacoes);
	}
	public void solicitarRecuperacao(String email) {
		try {
			transacao.executeWithoutResult(status -> {
				var id=contas.bloquearContaPorEmail(email);
				if(id==null)return;
				var conta=contas.consultarConta(id);
				if(conta!=null&&conta.ativo())emissor.emitir(id,conta.email(),FinalidadeToken.REDEFINIR_SENHA);
			});
		} catch(RuntimeException erro) {
			// Mesma resposta202 inclusive em falha de infraestrutura: não permitir enumeração por indisponibilidade da fila.
			LoggerFactory.getLogger(GerenciarEmailSenha.class).warn("Falha técnica na solicitação de recuperação; traceId={}",MDC.get("traceId"));
		}
	}
	@Transactional
	public void confirmar(String valor) {
		var token=validarEBloquear(valor,FinalidadeToken.CONFIRMAR_EMAIL);
		var agora=relogio.instant();tokens.confirmarEmail(token.usuario(),agora);tokens.consumir(token.id(),agora);
	}
	@Transactional
	public void redefinir(String valor,String senha) {
		var token=validarEBloquear(valor,FinalidadeToken.REDEFINIR_SENHA);
		politica.validar(senha);
		var agora=relogio.instant();tokens.redefinirSenha(token.usuario(),encoder.encode(senha),agora);
		tokens.consumir(token.id(),agora);tokens.anular(token.usuario(),FinalidadeToken.REDEFINIR_SENHA,agora);
	}
	private TokenContaStore.Registro validarEBloquear(String valor,FinalidadeToken finalidade) {
		String hash=EmitirTokenConta.hash(valor);var token=tokens.consultar(hash);
		if(token==null||!contas.bloquearConta(token.usuario()))throw new ExcecaoNegocio(CodigoErro.TOKEN_INVALIDO,finalidade.regra());
		token=tokens.consultar(hash);
		if(token==null)throw new ExcecaoNegocio(CodigoErro.TOKEN_INVALIDO,finalidade.regra());
		token.estado().validar(finalidade,relogio.instant());
		var conta=contas.consultarConta(token.usuario());
		if(conta==null||!conta.ativo())throw new ExcecaoNegocio(CodigoErro.TOKEN_INVALIDO,finalidade.regra());
		return token;
	}
}
