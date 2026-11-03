package com.br.dietamais.assinaturas.dominio;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.assinaturas.api.StatusAssinatura;
import java.time.*;
import org.junit.jupiter.api.Test;

class PoliticaEscritaAssinaturaTest {
	final Instant agora=Instant.parse("2026-10-25T12:00:00Z");
	final PoliticaEscritaAssinatura politica=new PoliticaEscritaAssinatura(Duration.ofDays(7));
	@Test void testeVencidoNaoDependeDeJobParaBloquear() {
		assertThat(politica.permite(StatusAssinatura.TESTE,agora.plusSeconds(1),agora)).isTrue();
		assertThat(politica.permite(StatusAssinatura.TESTE,agora,agora)).isFalse();
		assertThat(politica.permite(StatusAssinatura.TESTE,null,agora)).isFalse();
	}
	@Test void carenciaConfiguravelAcabaExatamenteNoPrazo() {
		assertThat(politica.permite(StatusAssinatura.EM_ATRASO,agora.minus(Duration.ofDays(7)).plusSeconds(1),agora)).isTrue();
		assertThat(politica.permite(StatusAssinatura.EM_ATRASO,agora.minus(Duration.ofDays(7)),agora)).isFalse();
		assertThat(politica.permite(StatusAssinatura.EM_ATRASO,null,agora)).isFalse();
		assertThat(new PoliticaEscritaAssinatura(Duration.ofDays(2)).permite(StatusAssinatura.EM_ATRASO,agora.minus(Duration.ofDays(3)),agora)).isFalse();
	}
	@Test void estadosEncerradosNaoPermitemEscritaEAtivaSegueEstado() {
		assertThat(politica.permite(StatusAssinatura.CANCELADA,agora.plusSeconds(1),agora)).isFalse();
		assertThat(politica.permite(StatusAssinatura.EXPIRADA,agora.plusSeconds(1),agora)).isFalse();
		assertThat(politica.permite(StatusAssinatura.ATIVA,null,agora)).isTrue();
	}
}
