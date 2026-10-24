package com.br.dietamais;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DietamaisApplicationTests {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void liquibaseCriaPlanosIniciais() {
		Integer limite = jdbcTemplate.queryForObject(
				"select limite_pacientes from plano where codigo = 'BASICO_20'", Integer.class);
		assertThat(limite).isEqualTo(20);
	}

}
