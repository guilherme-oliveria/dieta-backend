package com.br.dietamais.contas.dominio;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class BloqueioLoginTest {
	private final Instant agora = Instant.parse("2026-10-25T12:00:00Z");
	@Test void quintaFalhaBloqueiaDuranteQuinzeMinutos() {
		var estado = new BloqueioLogin(List.of(), null);
		for (int i = 0; i < 4; i++) estado = estado.falhar(agora.plusSeconds(i));
		assertThat(estado.bloqueado(agora.plusSeconds(4))).isFalse();
		estado = estado.falhar(agora.plusSeconds(4));
		assertThat(estado.bloqueadoAte()).isEqualTo(agora.plusSeconds(904));
		assertThat(estado.bloqueado(agora.plusSeconds(903))).isTrue();
		assertThat(estado.bloqueado(agora.plusSeconds(904))).isFalse();
	}
	@Test void janelaMovelDescartaFalhasQueJaCompletaramQuinzeMinutos() {
		var estado = new BloqueioLogin(List.of(agora.minusSeconds(900), agora.minusSeconds(899), agora.minusSeconds(1)), null);
		assertThat(estado.falhar(agora).falhas()).containsExactly(agora.minusSeconds(899), agora.minusSeconds(1), agora);
		assertThat(estado.falhar(agora).bloqueadoAte()).isNull();
	}
}
