package com.br.dietamais.contas.dominio;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.Test;

class LimiteTaxaLoginTest {
	@Test void limiteMovelPorIpRetornaEsperaSemLiberarAntesDaHora() {
		var limite = new LimiteTaxaLogin(10, 2);
		var agora = Instant.parse("2026-10-25T12:00:00Z");
		for (int i = 0; i < 10; i++) assertThat(limite.espera("ip-a", agora)).isZero();
		assertThat(limite.espera("ip-a", agora)).isEqualTo(60);
		assertThat(limite.espera("ip-a", agora.plusMillis(1))).isEqualTo(60);
		assertThat(limite.espera("ip-b", agora)).isZero();
		assertThat(limite.espera("ip-c", agora)).isEqualTo(60);
		assertThat(limite.espera("ip-a", agora.plusSeconds(60))).isZero();
	}
}
