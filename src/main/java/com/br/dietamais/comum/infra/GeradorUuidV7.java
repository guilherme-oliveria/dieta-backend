package com.br.dietamais.comum.infra;

import java.util.UUID;
import com.br.dietamais.comum.api.GeradorIds;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GeradorUuidV7 implements GeradorIds {
	private final JdbcTemplate jdbc;
	public GeradorUuidV7(JdbcTemplate jdbc) { this.jdbc = jdbc; }
	@Override public UUID novo() { return jdbc.queryForObject("select uuidv7()", UUID.class); }
}
