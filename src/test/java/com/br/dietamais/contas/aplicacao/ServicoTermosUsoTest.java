package com.br.dietamais.contas.aplicacao;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.dominio.DocumentoTermosUso;
import java.time.*;
import org.junit.jupiter.api.Test;

class ServicoTermosUsoTest {
	final Instant agora=Instant.parse("2026-10-26T12:00:00Z");
	ServicoTermosUso servico(String estado){return new ServicoTermosUso(()->new DocumentoTermosUso("1.0.0",estado,agora,agora,"Texto fictício de teste"),Clock.fixed(agora,ZoneOffset.UTC));}
	@Test void versaoPublicadaEAceiteExplicitoSaoObrigatorios() {
		var caso=servico("PUBLICADO");assertThatCode(()->caso.validar(true,"1.0.0")).doesNotThrowAnyException();
		for(Boolean aceitou:new Boolean[]{null,false})assertThatThrownBy(()->caso.validar(aceitou,"1.0.0")).isInstanceOfSatisfying(ExcecaoNegocio.class,erro->assertThat(erro.codigo()).isEqualTo(CodigoErro.TERMOS_NAO_ACEITOS));
		assertThatThrownBy(()->caso.validar(true,"0.9.0")).isInstanceOf(ExcecaoNegocio.class);
		assertThatThrownBy(()->servico("RASCUNHO").validar(true,"1.0.0")).isInstanceOfSatisfying(ExcecaoNegocio.class,erro->assertThat(erro.codigo()).isEqualTo(CodigoErro.TERMOS_INDISPONIVEIS));
	}
}
