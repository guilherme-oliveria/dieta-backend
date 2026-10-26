package com.br.dietamais;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
	@Bean @org.springframework.context.annotation.Primary
	com.br.dietamais.contas.infra.FonteTermosUso termosPublicadosFicticios() {
		return ()->new com.br.dietamais.contas.dominio.DocumentoTermosUso("2026-10","PUBLICADO",java.time.Instant.EPOCH,java.time.Instant.EPOCH,"FONTE FICTÍCIA EXCLUSIVA DE TESTES, SEM VALIDADE JURÍDICA");
	}
	@Bean
	static org.springframework.beans.factory.config.BeanFactoryPostProcessor semAgendamentoDeEmailReal() {
		return fabrica -> fabrica.getBean(org.springframework.core.env.ConfigurableEnvironment.class).getPropertySources()
				.addFirst(new org.springframework.core.env.MapPropertySource("emails-somente-testes",java.util.Map.of("dieta.notificacoes.envio-habilitado",false)));
	}
	@Bean @org.springframework.context.annotation.Primary
	com.br.dietamais.notificacoes.infra.CifradorEmail cifraEmailTeste() {
		byte[] bytes = new byte[32];new java.security.SecureRandom().nextBytes(bytes);
		return new com.br.dietamais.notificacoes.infra.CifradorEmail(java.util.Base64.getEncoder().encodeToString(bytes));
	}
	@Bean @org.springframework.context.annotation.Primary
	TransporteEmailTeste transporteEmailTeste() { return new TransporteEmailTeste(); }
	public static class TransporteEmailTeste implements com.br.dietamais.notificacoes.api.EnviadorEmail {
		public final java.util.List<com.br.dietamais.notificacoes.api.MensagemEmail> mensagens = new java.util.concurrent.CopyOnWriteArrayList<>();
		public volatile boolean falhar;
		@Override public void enviar(com.br.dietamais.notificacoes.api.MensagemEmail mensagem) {
			if(falhar)throw new IllegalStateException("Falha fictícia de transporte");
			mensagens.add(mensagem);
		}
	}

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
	}

}
