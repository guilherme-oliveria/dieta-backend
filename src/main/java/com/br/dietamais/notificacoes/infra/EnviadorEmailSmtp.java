package com.br.dietamais.notificacoes.infra;

import com.br.dietamais.notificacoes.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Component;

@Component
public class EnviadorEmailSmtp implements EnviadorEmail {
	private final JavaMailSender sender;
	private final String remetente;
	public EnviadorEmailSmtp(JavaMailSender sender,@Value("${EMAIL_REMETENTE:nao-responda@example.invalid}") String remetente) {
		this.sender=sender;this.remetente=remetente;
	}
	@Override public void enviar(MensagemEmail mensagem) {
		try {
			var mime=sender.createMimeMessage();var helper=new MimeMessageHelper(mime,false,"UTF-8");
			helper.setFrom(remetente);helper.setTo(mensagem.destinatario());helper.setSubject(mensagem.assunto());helper.setText(mensagem.html(),true);
			sender.send(mime);
		} catch(jakarta.mail.MessagingException erro) { throw new IllegalStateException("Não foi possível preparar o e-mail."); }
	}
}
