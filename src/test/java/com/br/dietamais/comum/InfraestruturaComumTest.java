package com.br.dietamais.comum;

import static org.assertj.core.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.br.dietamais.TestcontainersConfiguration;
import com.br.dietamais.comum.api.CodigoErro;
import com.br.dietamais.comum.api.ExcecaoNegocio;
import com.br.dietamais.comum.aplicacao.RespostaIdempotente;
import com.br.dietamais.comum.aplicacao.ServicoIdempotencia;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.liquibase.change-log=classpath:db/changelog/db.changelog-teste-comum.yaml")
@Import({ TestcontainersConfiguration.class, InfraestruturaComumTest.Configuracao.class })
class InfraestruturaComumTest {
	@Autowired JdbcTemplate jdbc;
	@Autowired PlatformTransactionManager transacoes;
	@Autowired JsonMapper mapper;
	@jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entidades;
	private final Instant agora = Instant.parse("2026-10-24T12:00:00Z");
	private final com.br.dietamais.comum.infra.CifradorResposta cifrador = gerarCifrador();

	private static com.br.dietamais.comum.infra.CifradorResposta gerarCifrador() {
		byte[] chave = new byte[32];
		new java.security.SecureRandom().nextBytes(chave);
		return new com.br.dietamais.comum.infra.CifradorResposta(java.util.Base64.getEncoder().encodeToString(chave));
	}

	@org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
	@org.springframework.boot.persistence.autoconfigure.EntityScan(basePackageClasses = {
			com.br.dietamais.DietamaisApplication.class, suporte.persistencia.EntidadeSincronizavelTeste.class })
	static class Configuracao {
		@org.springframework.context.annotation.Bean
		@org.springframework.context.annotation.Primary
		Clock relogioControlado() { return Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC); }
	}

	private ServicoIdempotencia servico(Instant instante) {
		return new ServicoIdempotencia(jdbc, transacoes, mapper, Clock.fixed(instante, ZoneOffset.UTC), Duration.ofHours(24), cifrador);
	}

	@Test
	void migracaoCriaSequenciaETabelasComUuidv7() {
		assertThat(jdbc.queryForObject("select nextval('seq_sync')", Long.class)).isPositive();
		var chave = UUID.randomUUID();
		UUID id = jdbc.queryForObject("insert into idempotencia (escopo, chave, hash_requisicao, criado_em, expira_em) "
				+ "values ('teste-migracao', ?, 'hash', current_timestamp, current_timestamp) returning id", UUID.class, chave);
		assertThat(id.version()).isEqualTo(7);
		assertThat(jdbc.queryForObject("select count(*) from log_acesso", Long.class)).isNotNull();
	}

	@Test
	void repetirChaveReproduzRespostaSemReexecutarOperacao() {
		var chave = UUID.randomUUID();
		var chamadas = new AtomicInteger();
		var servico = servico(agora);
		var primeira = servico.executar("usuario-a/PACIENTE", chave, "hash", () -> {
			chamadas.incrementAndGet();
			return resposta(201);
		});
		var repetida = servico.executar("usuario-a/PACIENTE", chave, "hash", () -> {
			throw new AssertionError("Não deve reexecutar");
		});
		assertThat(chamadas).hasValue(1);
		assertThat(repetida.status()).isEqualTo(primeira.status());
		assertThat(repetida.corpo()).isEqualTo(primeira.corpo());
		assertThat(repetida.cabecalhos()).isEqualTo(primeira.cabecalhos());
	}

	@Test
	void chaveNaoRevelaRespostaDeOutroUsuarioOuPerfil() {
		var chave = UUID.randomUUID();
		var chamadas = new AtomicInteger();
		for (String escopo : java.util.List.of("a/NUTRICIONISTA", "a/PACIENTE", "b/PACIENTE")) {
			servico(agora).executar(escopo, chave, "hash", () -> { chamadas.incrementAndGet(); return resposta(201); });
		}
		assertThat(chamadas).hasValue(3);
	}

	@Test
	void mesmaChaveComOutraRequisicaoRetornaConflito() {
		var chave = UUID.randomUUID();
		servico(agora).executar("teste-conflito", chave, "hash-a", () -> resposta(201));
		assertThatThrownBy(() -> servico(agora).executar("teste-conflito", chave, "hash-b", () -> resposta(201)))
				.isInstanceOfSatisfying(ExcecaoNegocio.class,
						erro -> assertThat(erro.codigo()).isEqualTo(CodigoErro.CHAVE_IDEMPOTENCIA_REUTILIZADA));
	}

	@Test
	void erroDesfazOperacaoEReservaDaChave() {
		var chave = UUID.randomUUID();
		var servico = servico(agora);
		servico.executar("teste-rollback", chave, "hash", () -> {
			jdbc.update("insert into log_acesso (acao, entidade, registro_id, ocorrido_em) values ('TESTE', 'teste', ?, ?)",
					chave, java.sql.Timestamp.from(agora));
			return resposta(422);
		});
		assertThat(jdbc.queryForObject("select count(*) from log_acesso where registro_id = ?", Long.class, chave)).isZero();
		assertThat(servico.executar("teste-rollback", chave, "hash", () -> resposta(201)).status()).isEqualTo(201);
	}

	@Test
	void chaveExpiradaPodeSerReutilizadaComRelogioControlado() {
		var chave = UUID.randomUUID();
		servico(agora).executar("teste-expiracao", chave, "antigo", () -> resposta(201));
		assertThat(servico(agora.plus(Duration.ofDays(1))).executar("teste-expiracao", chave, "novo", () -> resposta(202)).status())
				.isEqualTo(202);
	}

	@Test
	void limpezaRemoveRespostasExpiradasSemRemoverChavesValidas() {
		var expirada = UUID.randomUUID();
		var valida = UUID.randomUUID();
		servico(agora).executar("teste-limpeza", expirada, "hash", () -> resposta(201));
		servico(agora.plusSeconds(1)).executar("teste-limpeza", valida, "hash", () -> resposta(201));
		new com.br.dietamais.comum.infra.LimpezaIdempotencia(jdbc,
				Clock.fixed(agora.plus(Duration.ofDays(1)), ZoneOffset.UTC)).removerExpiradas();
		assertThat(jdbc.queryForObject("select count(*) from idempotencia where chave = ?", Long.class, expirada)).isZero();
		assertThat(jdbc.queryForObject("select count(*) from idempotencia where chave = ?", Long.class, valida)).isEqualTo(1);
	}

	@Test
	void duasRequisicoesConcorrentesExecutamExatamenteUmaOperacao() throws Exception {
		var chave = UUID.randomUUID();
		var chamadas = new AtomicInteger();
		try (var executor = Executors.newFixedThreadPool(2)) {
			var tarefa = (java.util.concurrent.Callable<RespostaIdempotente>) () -> servico(agora)
					.executar("teste-concorrencia", chave, "hash", () -> { chamadas.incrementAndGet(); return resposta(201); });
			for (var resultado : executor.invokeAll(java.util.List.of(tarefa, tarefa))) {
				assertThat(resultado.get().status()).isEqualTo(201);
			}
		}
		assertThat(chamadas).hasValue(1);
	}

	@Test
	void entidadeJpaVersionaAtualizacoesEExclusaoLogicaComSequenciaGlobal() {
		UUID id = UUID.randomUUID();
		var transacao = new org.springframework.transaction.support.TransactionTemplate(transacoes);
		long seqInicial = transacao.execute(status -> {
			var entidade = new suporte.persistencia.EntidadeSincronizavelTeste(id);
			entidades.persist(entidade);
			entidades.flush();
			assertThat(entidade.getVersao()).isZero();
			assertThat(entidade.getCriadoEm()).isEqualTo(agora);
			assertThat(entidade.getAtualizadoEm()).isEqualTo(entidade.getCriadoEm());
			return entidade.getSeq();
		});
		long seqAtualizada = transacao.execute(status -> {
			var entidade = entidades.find(suporte.persistencia.EntidadeSincronizavelTeste.class, id);
			entidade.alterarDescricao("alterado");
			entidades.flush();
			assertThat(entidade.getVersao()).isEqualTo(1);
			assertThat(entidade.getSeq()).isGreaterThan(seqInicial);
			return entidade.getSeq();
		});
		transacao.executeWithoutResult(status -> {
			var entidade = entidades.find(suporte.persistencia.EntidadeSincronizavelTeste.class, id);
			entidade.excluir(agora);
			entidades.flush();
			assertThat(entidade.getVersao()).isEqualTo(2);
			assertThat(entidade.getSeq()).isGreaterThan(seqAtualizada);
			assertThat(entidade.getExcluidoEm()).isEqualTo(agora);
		});
	}

	private RespostaIdempotente resposta(int status) {
		return new RespostaIdempotente(status, Map.of("Content-Type", java.util.List.of("application/json")), "{\"ok\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}
}
