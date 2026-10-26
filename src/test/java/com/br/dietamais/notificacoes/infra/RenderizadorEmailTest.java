package com.br.dietamais.notificacoes.infra;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.notificacoes.api.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class RenderizadorEmailTest {
	RenderizadorEmail renderizador(String url) {
		var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");
		var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);
		return new RenderizadorEmail(engine,url,"https://app.example.invalid/redefinir-senha");
	}
	@Test void templatesTemLinkConfiguradoEPrazosSemDadosClinicos() {
		var renderizador=renderizador("dietamais://confirmar-email");
		var confirmar=renderizador.renderizar(new EmailTransacional(TipoEmail.CONFIRMAR_EMAIL,"ficticio@example.invalid","TOKEN_SEM_VALIDADE",Instant.parse("2026-10-26T12:00:00Z")));
		assertThat(confirmar.html()).contains("dietamais://confirmar-email?token=TOKEN_SEM_VALIDADE","24 horas").doesNotContain("th:href=");
		var redefinir=renderizador.renderizar(new EmailTransacional(TipoEmail.REDEFINIR_SENHA,"ficticio@example.invalid","TOKEN_SEM_VALIDADE",Instant.parse("2026-10-25T13:00:00Z")));
		assertThat(redefinir.html()).contains("https://app.example.invalid/redefinir-senha?token=TOKEN_SEM_VALIDADE","uma hora");
		assertThat(redefinir.toString()).doesNotContain("ficticio@example.invalid","TOKEN_SEM_VALIDADE");
	}
	@Test void linksNaoAceitamScriptOuHostDerivadoDaRequisicao() {
		assertThatThrownBy(()->renderizador("javascript:SEM_VALIDADE")).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(()->renderizador("https://app.example.invalid?token=SEM_VALIDADE")).isInstanceOf(IllegalStateException.class);
	}
	@Test void conviteTemCodigoEPrazoSemIdentidadeOuDadosClinicosNoTexto() {
		var convite=renderizador("dietamais://confirmar-email").renderizar(new EmailTransacional(TipoEmail.CONVITE,"ficticio@example.invalid","AB2CD3EF",Instant.parse("2026-11-02T12:00:00Z")));
		assertThat(convite.html()).contains("dietamais://convites/AB2CD3EF","2026-11-02T12:00:00Z").doesNotContain("ficticio@example.invalid","redefinir-senha");
		assertThat(convite.assunto()).contains("convite");
	}
}
