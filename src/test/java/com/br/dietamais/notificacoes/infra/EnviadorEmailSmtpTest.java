package com.br.dietamais.notificacoes.infra;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.br.dietamais.notificacoes.api.MensagemEmail;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class EnviadorEmailSmtpTest {
	@Test void preparaMimeUtf8EEntregaSomenteAoTransporteInjetado() throws Exception {
		var sender=mock(JavaMailSender.class);
		var mime=new MimeMessage(Session.getInstance(new java.util.Properties()));
		when(sender.createMimeMessage()).thenReturn(mime);
		new EnviadorEmailSmtp(sender,"nao-responda@example.invalid").enviar(new MensagemEmail("ficticio@example.invalid","Confirmação fictícia","<p>Conteúdo fictício</p>"));
		assertThat(mime.getSubject()).isEqualTo("Confirmação fictícia");
		assertThat(mime.getAllRecipients()).extracting(Object::toString).containsExactly("ficticio@example.invalid");
		assertThat(mime.getContent().toString()).contains("Conteúdo fictício");
		verify(sender).send(mime);
	}
}
