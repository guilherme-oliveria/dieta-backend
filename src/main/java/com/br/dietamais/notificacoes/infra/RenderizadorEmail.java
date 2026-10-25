package com.br.dietamais.notificacoes.infra;

import com.br.dietamais.notificacoes.api.*;
import java.net.URI;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class RenderizadorEmail {
	private final TemplateEngine engine;
	private final String confirmacao;
	private final String redefinicao;
	public RenderizadorEmail(TemplateEngine engine,@Value("${EMAIL_URL_CONFIRMAR:dietamais://confirmar-email}") String confirmacao,
			@Value("${EMAIL_URL_REDEFINIR:dietamais://redefinir-senha}") String redefinicao) {
		this.engine=engine;this.confirmacao=validar(confirmacao);this.redefinicao=validar(redefinicao);
	}
	private String validar(String endereco) {
		var uri=URI.create(endereco);
		if(!java.util.Set.of("https","dietamais").contains(uri.getScheme()) || uri.getHost()==null || uri.getRawUserInfo()!=null
				||uri.getRawFragment()!=null||uri.getRawQuery()!=null) throw new IllegalStateException("Configure links de e-mail HTTPS ou do app, sem query/fragmento.");
		return endereco;
	}
	public MensagemEmail renderizar(EmailTransacional email) {
		boolean confirma=email.tipo()==TipoEmail.CONFIRMAR_EMAIL;
		String link=UriComponentsBuilder.fromUriString(confirma?confirmacao:redefinicao).queryParam("token",email.token()).build().encode().toUriString();
		var contexto=new Context(Locale.forLanguageTag("pt-BR"));
		contexto.setVariable("link",link);
		String html=engine.process(confirma?"email/confirmar-email":"email/redefinir-senha",contexto);
		return new MensagemEmail(email.destinatario(),confirma?"Confirme seu e-mail no Dieta+":"Redefina sua senha no Dieta+",html);
	}
}
