package com.br.dietamais;

import com.br.dietamais.comum.infra.CifradorResposta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class IdempotenciaTesteConfigTest {
	@Test void fallbackGeraChaveSomenteNoContextoDeTestes(){
		var ambiente=new MockEnvironment();configurar(ambiente);assertThat(new CifradorResposta(ambiente.getRequiredProperty("IDEMPOTENCIA_CHAVE")).autenticarRequisicao(new byte[0])).isPresent();
	}
	@Test void propriedadeVaziaExplicitaContinuaSemChave(){
		var ambiente=new MockEnvironment().withProperty("IDEMPOTENCIA_CHAVE","");configurar(ambiente);assertThat(new CifradorResposta(ambiente.getRequiredProperty("IDEMPOTENCIA_CHAVE")).autenticarRequisicao(new byte[0])).isEmpty();
	}
	void configurar(MockEnvironment ambiente){var fabrica=new DefaultListableBeanFactory();fabrica.registerSingleton("ambiente",ambiente);TestcontainersConfiguration.semAgendamentoDeEmailReal().postProcessBeanFactory(fabrica);}
}
