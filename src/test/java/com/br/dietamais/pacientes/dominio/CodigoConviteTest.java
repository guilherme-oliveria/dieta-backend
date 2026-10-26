package com.br.dietamais.pacientes.dominio;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class CodigoConviteTest {
	@Test void geraOitoCaracteresSemAmbiguidadeEArmazenaSomenteHash() {
		var gerador=new CodigoConvite();var codigos=new HashSet<String>();
		for(int i=0;i<1000;i++){String codigo=gerador.novo();assertThat(codigo).matches("[A-HJ-NP-Z2-9]{8}");codigos.add(codigo);assertThat(CodigoConvite.hash(codigo)).hasSize(64).doesNotContain(codigo);}
		assertThat(codigos).hasSize(1000);
	}
	@Test void prazoExatoExpiraEReusoEInvalidacaoNaoSaoAceitos() {
		var fim=Instant.parse("2026-11-01T12:00:00Z");
		assertThatCode(()->CodigoConvite.validar(false,false,fim,fim.minusNanos(1))).doesNotThrowAnyException();
		assertThatThrownBy(()->CodigoConvite.validar(false,false,fim,fim)).hasMessageContaining("expirou");
		assertThatThrownBy(()->CodigoConvite.validar(true,false,fim,fim.minusSeconds(1))).hasMessageContaining("inválido");
		assertThatThrownBy(()->CodigoConvite.validar(false,true,fim,fim.minusSeconds(1))).hasMessageContaining("inválido");
	}
}
