package com.br.dietamais.sync.dominio;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.api.*;
import org.junit.jupiter.api.Test;

class CursorSyncTest {
	@Test void aceitaDecimalLongOuCargaInicial() {
		assertThat(CursorSync.ler(null)).isZero();assertThat(CursorSync.ler("")).isZero();assertThat(CursorSync.ler("00042")).isEqualTo(42);assertThat(CursorSync.ler(Long.toString(Long.MAX_VALUE))).isEqualTo(Long.MAX_VALUE);
	}
	@Test void rejeitaCursorInvalidoOuFuturoELimiteForaDoIntervalo() {
		for(String cursor:new String[]{" ","-1","+1","1.5","9223372036854775808"})assertThatThrownBy(()->CursorSync.ler(cursor)).isInstanceOf(ExcecaoNegocio.class);
		assertThatThrownBy(()->CursorSync.validar(11,10,1)).isInstanceOf(ExcecaoNegocio.class);
		for(int limite:new int[]{0,1001})assertThatThrownBy(()->CursorSync.validar(0,10,limite)).isInstanceOf(ExcecaoNegocio.class);
	}
}
