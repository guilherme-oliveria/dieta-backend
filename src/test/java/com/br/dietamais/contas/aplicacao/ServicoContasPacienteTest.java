package com.br.dietamais.contas.aplicacao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.dominio.PoliticaSenha;
import com.br.dietamais.contas.infra.SessaoStore;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

class ServicoContasPacienteTest {
	@Test void consentimentoClinicoNaoSubstituiGuardaDosTermosDeUso() {
		var jdbc=mock(JdbcTemplate.class);var politica=mock(PoliticaSenha.class);var encoder=mock(PasswordEncoder.class);var ids=mock(GeradorIds.class);var emissor=mock(EmitirTokenConta.class);var termos=mock(TermosUso.class);
		doThrow(new ExcecaoNegocio(CodigoErro.TERMOS_NAO_ACEITOS,"RN-CON-09")).when(termos).validar(null,null);
		var caso=new ServicoContasPaciente(jdbc,mock(SessaoStore.class),politica,encoder,ids,Clock.systemUTC(),mock(AutenticarSessao.class),emissor,"America/Sao_Paulo",termos);
		assertThatThrownBy(()->caso.criar("Fictício","ficticio@example.invalid","Senha fictícia longa",null,null,null)).isInstanceOfSatisfying(ExcecaoNegocio.class,erro->assertThat(erro.codigo()).isEqualTo(CodigoErro.TERMOS_NAO_ACEITOS));
		verifyNoInteractions(jdbc,politica,encoder,ids,emissor);
	}
}
