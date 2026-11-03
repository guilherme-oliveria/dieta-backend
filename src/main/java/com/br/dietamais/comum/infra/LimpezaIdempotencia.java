package com.br.dietamais.comum.infra;

import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LimpezaIdempotencia {
	private final JdbcTemplate jdbc;
	private final Clock relogio;

	public LimpezaIdempotencia(JdbcTemplate jdbc, Clock relogio) {
		this.jdbc = jdbc;
		this.relogio = relogio;
	}

	@Scheduled(fixedDelayString = "${dieta.idempotencia.intervalo-limpeza:PT1H}",
			initialDelayString = "${dieta.idempotencia.intervalo-limpeza:PT1H}")
	public void removerExpiradas() {
		jdbc.update("delete from idempotencia where expira_em <= ?", Timestamp.from(relogio.instant()));
	}
}
