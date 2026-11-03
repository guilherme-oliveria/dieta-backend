package com.br.dietamais.contas.infra;

import com.br.dietamais.contas.dominio.DocumentoTermosUso;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class FonteTermosUsoMarkdown implements FonteTermosUso {
	private final DocumentoTermosUso documento;
	public FonteTermosUsoMarkdown() {
		try(var fonte=new ClassPathResource("legal/terms-of-use.md").getInputStream()){documento=DocumentoTermosUso.ler(new String(fonte.readAllBytes(),StandardCharsets.UTF_8));}
		catch(IOException erro){throw new IllegalStateException("Fonte interna dos termos de uso indisponível",erro);}
	}
	@Override public DocumentoTermosUso obter(){return documento;}
}
