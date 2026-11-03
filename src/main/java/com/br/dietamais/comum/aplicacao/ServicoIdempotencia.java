package com.br.dietamais.comum.aplicacao;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.br.dietamais.comum.api.CodigoErro;
import com.br.dietamais.comum.api.ExcecaoNegocio;
import com.br.dietamais.comum.infra.CifradorResposta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ServicoIdempotencia {
	private final JdbcTemplate jdbc;
	private final TransactionTemplate transacao;
	private final JsonMapper mapper;
	private final Clock relogio;
	private final Duration prazo;
	private final CifradorResposta cifrador;

	public ServicoIdempotencia(JdbcTemplate jdbc, PlatformTransactionManager transacoes, JsonMapper mapper,
			Clock relogio, @Value("${dieta.idempotencia.prazo:PT24H}") Duration prazo, CifradorResposta cifrador) {
		if (prazo.isNegative() || prazo.isZero()) throw new IllegalArgumentException("Prazo de idempotência deve ser positivo");
		this.jdbc = jdbc;
		this.transacao = new TransactionTemplate(transacoes);
		this.mapper = mapper;
		this.relogio = relogio;
		this.prazo = prazo;
		this.cifrador = cifrador;
	}

	public RespostaIdempotente executar(String escopo, UUID chave, String hash, Supplier<RespostaIdempotente> operacao) {
		return transacao.execute(status -> {
			var agora = relogio.instant();
			// O índice único bloqueia concorrentes até o commit da operação e da resposta.
			var reservas = jdbc.queryForList("""
					insert into idempotencia (escopo, chave, hash_requisicao, criado_em, expira_em)
					values (?, ?, ?, ?, ?)
					on conflict (escopo, chave) do update set hash_requisicao = excluded.hash_requisicao,
					criado_em = excluded.criado_em, expira_em = excluded.expira_em,
					status_http = null, cabecalhos_resposta = null, corpo_resposta = null
					where idempotencia.expira_em <= excluded.criado_em returning id
					""", UUID.class, escopo, chave, hash, Timestamp.from(agora), Timestamp.from(agora.plus(prazo)));
			if (reservas.isEmpty()) return recuperar(escopo, chave, hash);
			var resposta = operacao.get();
			if (resposta.status() >= 200 && resposta.status() < 300) {
				jdbc.update("update idempotencia set status_http = ?, cabecalhos_resposta = ?, corpo_resposta = ? where id = ?",
						resposta.status(), mapper.writeValueAsString(resposta.cabecalhos()),
						cifrador.cifrar(resposta.corpo(), contexto(escopo, chave, hash)), reservas.getFirst());
			} else {
				status.setRollbackOnly();
			}
			return resposta;
		});
	}

	private RespostaIdempotente recuperar(String escopo, UUID chave, String hash) {
		return jdbc.queryForObject("select hash_requisicao, status_http, cabecalhos_resposta, corpo_resposta "
				+ "from idempotencia where escopo = ? and chave = ?", (linha, numero) -> {
			if (!hash.equals(linha.getString("hash_requisicao"))) {
				throw new ExcecaoNegocio(CodigoErro.CHAVE_IDEMPOTENCIA_REUTILIZADA, null);
			}
			int status = linha.getInt("status_http");
			if (linha.wasNull()) throw new IllegalStateException("Registro de idempotência sem resposta");
			Map<String, List<String>> cabecalhos = mapper.readValue(linha.getString("cabecalhos_resposta"), new TypeReference<>() { });
			return new RespostaIdempotente(status, cabecalhos,
					cifrador.decifrar(linha.getBytes("corpo_resposta"), contexto(escopo, chave, hash)));
		}, escopo, chave);
	}

	private String contexto(String escopo, UUID chave, String hash) { return escopo + "\n" + chave + "\n" + hash; }
}
