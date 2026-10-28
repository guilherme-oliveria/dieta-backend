package com.br.dietamais.comum;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.infra.*;
import java.nio.file.*;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class ArmazenamentoTest {
	@Test void s3AssinaUrlConformeVetorDaDocumentacaoDaAws() throws Exception {
		var s3=new ArmazenamentoS3("https://examplebucket.s3.amazonaws.com","examplebucket","us-east-1","AKIAIOSFODNN7EXAMPLE","wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",Clock.systemUTC());
		var m=ArmazenamentoS3.class.getDeclaredMethod("presign",String.class,String.class,String.class,Instant.class,long.class,String.class,String.class);
		m.setAccessible(true);
		var url=(String)m.invoke(s3,"GET","examplebucket.s3.amazonaws.com","/test.txt",Instant.parse("2013-05-24T00:00:00Z"),86400L,null,null);
		assertThat(url).endsWith("X-Amz-Signature=aeeed9bbccd4d02ee5c0109b86d86835f995330da4c265957d157751f604d404");
		assertThat(url).contains("X-Amz-SignedHeaders=host").contains("X-Amz-Expires=86400");
	}

	@Test void localGuardaLeApagaEAssinaComValidade(@TempDir Path dir) throws Exception {
		var agora=new java.util.concurrent.atomic.AtomicReference<>(Instant.parse("2026-10-28T12:00:00Z"));
		Clock relogio=new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return agora.get();}};
		var a=new ArmazenamentoLocal(dir,"segredo".getBytes(),"https://api.invalid/",relogio);
		a.guardar("pdf/x.pdf","conteudo".getBytes(),"application/pdf");
		// Files.write usa o relógio do sistema; a fixture deve acompanhar o Clock controlado do teste.
		Files.setLastModifiedTime(dir.resolve("pdf/x.pdf"),java.nio.file.attribute.FileTime.from(agora.get()));
		assertThat(a.ler("pdf/x.pdf")).hasValueSatisfying(b->assertThat(new String(b)).isEqualTo("conteudo"));
		var acesso=a.urlAssinada("pdf/x.pdf",Duration.ofMinutes(10),"dieta.pdf","application/pdf");
		assertThat(acesso.url()).startsWith("https://api.invalid/api/v1/arquivos/");assertThat(acesso.expiraEm()).isEqualTo(agora.get().plusSeconds(600));
		var q=java.net.URI.create(acesso.url()).getQuery();
		var token=acesso.url().substring(acesso.url().indexOf("/arquivos/")+10,acesso.url().indexOf('?'));
		var sig=q.substring(q.indexOf("sig=")+4);
		assertThat(a.validar(token,acesso.expiraEm().getEpochSecond(),"dieta.pdf","application/pdf",sig)).contains("pdf/x.pdf");
		assertThat(a.validar(token,acesso.expiraEm().getEpochSecond(),"outro.pdf","application/pdf",sig)).isEmpty();
		assertThat(a.validar(token,acesso.expiraEm().getEpochSecond()+1,"dieta.pdf","application/pdf",sig)).isEmpty();
		agora.set(agora.get().plusSeconds(601));
		assertThat(a.validar(token,acesso.expiraEm().getEpochSecond(),"dieta.pdf","application/pdf",sig)).isEmpty(); // venceu
		assertThat(a.apagarAntigos("pdf",Duration.ofSeconds(1))).isEqualTo(1);
		assertThat(a.ler("pdf/x.pdf")).isEmpty();
		assertThatThrownBy(()->a.guardar("../fora.txt",new byte[0],"text/plain")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(()->a.ler("/etc/passwd")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test void limpezaRemoveAntigoEPreservaArquivoNoLimiteERecente(@TempDir Path dir) throws Exception {
		var agora=Instant.parse("2026-10-28T12:00:00Z");var idade=Duration.ofMinutes(10);var limite=agora.minus(idade);
		var a=new ArmazenamentoLocal(dir,"segredo".getBytes(),"https://api.invalid/",Clock.fixed(agora,ZoneOffset.UTC));
		var datas=java.util.Map.of("antigo",limite.minusSeconds(1),"limite",limite,"recente",limite.plusSeconds(1));
		for(var entrada:datas.entrySet()){
			String chave="pdf/"+entrada.getKey()+".pdf";a.guardar(chave,entrada.getKey().getBytes(),"application/pdf");
			Files.setLastModifiedTime(dir.resolve(chave),java.nio.file.attribute.FileTime.from(entrada.getValue()));
		}
		assertThat(a.apagarAntigos("pdf",idade)).isEqualTo(1);
		assertThat(a.ler("pdf/antigo.pdf")).isEmpty();
		assertThat(a.ler("pdf/limite.pdf")).hasValueSatisfying(bytes->assertThat(new String(bytes)).isEqualTo("limite"));
		assertThat(a.ler("pdf/recente.pdf")).hasValueSatisfying(bytes->assertThat(new String(bytes)).isEqualTo("recente"));
		assertThat(a.apagarAntigos("pdf",idade)).isZero();
	}
}
