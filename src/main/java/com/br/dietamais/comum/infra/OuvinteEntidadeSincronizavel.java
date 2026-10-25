package com.br.dietamais.comum.infra;

import java.time.Clock;
import com.br.dietamais.comum.api.EntidadeSincronizavel;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OuvinteEntidadeSincronizavel {
	private final JdbcTemplate jdbc;
	private final Clock relogio;

	public OuvinteEntidadeSincronizavel(JdbcTemplate jdbc, Clock relogio) {
		this.jdbc = jdbc;
		this.relogio = relogio;
	}

	@PrePersist
	@PreUpdate
	public void antesDeEscrever(EntidadeSincronizavel entidade) {
		long sequencia = jdbc.queryForObject("select nextval('seq_sync')", Long.class);
		entidade.registrarEscrita(sequencia, relogio.instant());
	}
}
