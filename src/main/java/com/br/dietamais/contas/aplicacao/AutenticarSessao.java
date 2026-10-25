package com.br.dietamais.contas.aplicacao;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.api.LoginRequisicao;
import com.br.dietamais.contas.dominio.BloqueioLogin;
import com.br.dietamais.contas.infra.SessaoStore;
import com.br.dietamais.contas.infra.TokensJwt;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AutenticarSessao {
	private final SessaoStore store;
	private final PasswordEncoder encoder;
	private final TokensJwt jwt;
	private final GeradorIds ids;
	private final Clock relogio;
	private final TransactionTemplate transacao;
	private final SecureRandom aleatorio = new SecureRandom();
	private final String hashAusente;
	public AutenticarSessao(SessaoStore store, PasswordEncoder encoder, TokensJwt jwt, GeradorIds ids,
			Clock relogio, PlatformTransactionManager transacoes) {
		this.store = store;
		this.encoder = encoder;
		this.jwt = jwt;
		this.ids = ids;
		this.relogio = relogio;
		this.transacao = new TransactionTemplate(transacoes);
		hashAusente = encoder.encode(UUID.randomUUID().toString());
	}

	public SessaoResposta entrar(LoginRequisicao entrada) {
		return resposta(transacao.execute(status -> {
			String identificador = hash(entrada.email());
			var tentativas = store.bloquearTentativas(identificador, relogio.instant());
			var agora = relogio.instant();
			if (tentativas.bloqueado(agora)) return Resultado.bloqueado(tentativas.bloqueadoAte());
			var id = store.bloquearContaPorEmail(entrada.email());
			var conta = id == null ? null : store.consultarConta(id);
			boolean confere = encoder.matches(entrada.senha(), conta == null ? hashAusente : conta.senhaHash());
			if (!confere || conta == null || !conta.ativo() || conta.perfis().isEmpty()) {
				var estado = tentativas.falhar(agora);
				store.salvarTentativas(identificador, estado, agora);
				return estado.bloqueado(agora) ? Resultado.bloqueado(estado.bloqueadoAte()) : Resultado.erro(CodigoErro.CREDENCIAIS_INVALIDAS);
			}
			store.salvarTentativas(identificador, new BloqueioLogin(List.of(), null), agora);
			var perfil = entrada.perfil();
			if (perfil == null && conta.perfis().size() == 1) perfil = conta.perfis().getFirst();
			if (perfil == null) return Resultado.sucesso(new SessaoResposta(null, null, null, conta.resposta(null)));
			if (!conta.perfis().contains(perfil)) return Resultado.erro(CodigoErro.PERFIL_INVALIDO);
			store.revogarDispositivo(conta.id(), entrada.dispositivo().id(), agora);
			return Resultado.sucesso(emitir(conta, perfil, entrada.dispositivo(), ids.novo(), agora.plus(Duration.ofDays(30)), agora));
		}));
	}

	public SessaoResposta renovar(String valor) {
		return resposta(transacao.execute(status -> {
			var token = refreshBloqueado(valor);
			var agora = relogio.instant();
			if (token == null) return Resultado.erro(CodigoErro.TOKEN_INVALIDO);
			if (token.consumido() || token.revogado()) {
				store.revogarFamilia(token.familia(), agora);
				return Resultado.erro(CodigoErro.TOKEN_INVALIDO);
			}
			var conta = store.consultarConta(token.usuario());
			if (!agora.isBefore(token.expiraEm()) || conta == null || !conta.ativo() || !conta.perfis().contains(token.perfil())) {
				store.revogarFamilia(token.familia(), agora);
				return Resultado.erro(CodigoErro.TOKEN_INVALIDO);
			}
			store.consumir(token.id(), agora);
			var dispositivo = new DispositivoRequisicao(token.dispositivo(), token.plataforma(), token.nomeAparelho());
			return Resultado.sucesso(emitir(conta, token.perfil(), dispositivo, token.familia(), token.expiraEm(), agora));
		}));
	}

	public void sair(String valor) {
		transacao.executeWithoutResult(status -> {
			var token = refreshBloqueado(valor);
			if (token != null) store.revogarFamilia(token.familia(), relogio.instant());
		});
	}

	private SessaoStore.Refresh refreshBloqueado(String valor) {
		String hash = hash(valor);
		var token = store.consultarRefresh(hash);
		if (token == null || !store.bloquearConta(token.usuario())) return null;
		// O lock do usuário serializa rotação, logout e login do mesmo dispositivo; relê após o lock.
		return store.consultarRefresh(hash);
	}
	private SessaoResposta emitir(SessaoStore.Conta conta, Perfil perfil, DispositivoRequisicao dispositivo, UUID familia, Instant fim, Instant agora) {
		byte[] bytes = new byte[32];
		aleatorio.nextBytes(bytes);
		String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		store.guardar(ids.novo(), conta.id(), familia, dispositivo, perfil, hash(refresh), agora, fim);
		var access = jwt.emitir(conta.id(), perfil, conta.nid(), conta.pid());
		return new SessaoResposta(access.valor(), refresh, access.expiraEm(), conta.resposta(perfil));
	}
	private SessaoResposta resposta(Resultado resultado) {
		Objects.requireNonNull(resultado);
		// Falhas persistentes (bloqueio/reuso) são confirmadas antes de produzir a exceção HTTP.
		if (resultado.liberarEm() != null) throw new LoginBloqueado(resultado.liberarEm());
		if (resultado.erro() != null) throw new ExcecaoNegocio(resultado.erro(), resultado.erro() == CodigoErro.CREDENCIAIS_INVALIDAS ? "RN-CON-03" : null);
		return resultado.sessao();
	}
	private static String hash(String valor) {
		try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8))); }
		catch (NoSuchAlgorithmException erro) { throw new IllegalStateException("SHA-256 indisponível"); }
	}
	private record Resultado(SessaoResposta sessao, CodigoErro erro, Instant liberarEm) {
		static Resultado sucesso(SessaoResposta sessao) { return new Resultado(sessao, null, null); }
		static Resultado erro(CodigoErro erro) { return new Resultado(null, erro, null); }
		static Resultado bloqueado(Instant ate) { return new Resultado(null, null, ate); }
		@Override public String toString() { return "Resultado[credenciais protegidas]"; }
	}
}
