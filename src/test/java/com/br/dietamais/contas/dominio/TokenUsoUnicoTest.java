package com.br.dietamais.contas.dominio;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.api.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TokenUsoUnicoTest {
	final Instant agora = Instant.parse("2026-10-25T12:00:00Z");
	@Test void somenteFinalidadeCertaAntesDoPrazoESemUsoOuAnulacaoEValida() {
		new TokenUsoUnico(FinalidadeToken.CONFIRMAR_EMAIL,agora.plusSeconds(1),false,false).validar(FinalidadeToken.CONFIRMAR_EMAIL,agora);
		for(var token : java.util.List.of(
				new TokenUsoUnico(FinalidadeToken.CONFIRMAR_EMAIL,agora,false,false),
				new TokenUsoUnico(FinalidadeToken.CONFIRMAR_EMAIL,agora.plusSeconds(1),true,false),
				new TokenUsoUnico(FinalidadeToken.CONFIRMAR_EMAIL,agora.plusSeconds(1),false,true),
				new TokenUsoUnico(FinalidadeToken.REDEFINIR_SENHA,agora.plusSeconds(1),false,false))) {
			assertThatThrownBy(() -> token.validar(FinalidadeToken.CONFIRMAR_EMAIL,agora)).isInstanceOfSatisfying(ExcecaoNegocio.class,
					erro -> {assertThat(erro.codigo()).isEqualTo(CodigoErro.TOKEN_INVALIDO);assertThat(erro.regra()).isEqualTo("RN-CON-05");});
		}
	}
}
