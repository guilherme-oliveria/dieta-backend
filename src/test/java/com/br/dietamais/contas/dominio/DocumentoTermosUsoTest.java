package com.br.dietamais.contas.dominio;

import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DocumentoTermosUsoTest {
	static final Instant AGORA=Instant.parse("2026-10-26T12:00:00Z");
	@Test void exigeAprovacaoEPublicacaoEfetivas() {
		assertThat(new DocumentoTermosUso("1.0.0","RASCUNHO",null,null,"Rascunho").habilitado(AGORA)).isFalse();
		assertThat(new DocumentoTermosUso("1.0.0","APROVADO",AGORA,null,"Teste").habilitado(AGORA)).isFalse();
		assertThat(new DocumentoTermosUso("1.0.0","PUBLICADO",null,AGORA,"Teste").habilitado(AGORA)).isFalse();
		assertThat(new DocumentoTermosUso("1.0.0","PUBLICADO",AGORA,AGORA.plusSeconds(1),"Teste").habilitado(AGORA)).isFalse();
		assertThat(new DocumentoTermosUso("1.0.0","PUBLICADO",AGORA,AGORA,"Teste").habilitado(AGORA)).isTrue();
	}
	@Test void fontePreservaVersaoEstadoEConteudoSemAprovarPorDefault() {
		var doc=DocumentoTermosUso.ler("---\nversao: 1.0.0\nstatus: RASCUNHO\naprovadoEm:\npublicadoEm:\n---\n# Conteúdo pendente\n");
		assertThat(doc.versao()).isEqualTo("1.0.0");assertThat(doc.status()).isEqualTo("RASCUNHO");assertThat(doc.conteudo()).contains("# Conteúdo pendente");assertThat(doc.habilitado(AGORA)).isFalse();
		assertThatThrownBy(()->DocumentoTermosUso.ler("SEM METADADOS")).isInstanceOf(IllegalArgumentException.class);
	}
}
