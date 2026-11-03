package com.br.dietamais.comum.infra;

import com.br.dietamais.comum.api.Armazenamento;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Disco local com URL assinada por HMAC, servida por {@link ArquivoController}. Chaves viram caminho sob {@code raiz}, sem sair dela. */
public class ArmazenamentoLocal implements Armazenamento {
	private final Path raiz;private final byte[] segredo;private final String urlBase;private final Clock relogio;
	public ArmazenamentoLocal(Path raiz,byte[] segredo,String urlBase,Clock relogio) {
		this.raiz=raiz.toAbsolutePath().normalize();this.segredo=segredo;this.urlBase=urlBase.endsWith("/")?urlBase.substring(0,urlBase.length()-1):urlBase;this.relogio=relogio;
	}
	private Path caminho(String chave) {
		if(chave==null||chave.isBlank()||chave.contains("..")||chave.startsWith("/")||!chave.matches("[A-Za-z0-9._/-]{1,200}"))throw new IllegalArgumentException("Chave inválida.");
		var p=raiz.resolve(chave).normalize();if(!p.startsWith(raiz))throw new IllegalArgumentException("Chave inválida.");return p;
	}
	@Override public void guardar(String chave,byte[] conteudo,String tipo) {
		try { var p=caminho(chave);Files.createDirectories(p.getParent());Files.write(p,conteudo); }
		catch (IOException erro) { throw new UncheckedIOException(erro); }
	}
	@Override public Optional<byte[]> ler(String chave) {
		try { var p=caminho(chave);return Files.isRegularFile(p)?Optional.of(Files.readAllBytes(p)):Optional.empty(); }
		catch (IOException erro) { throw new UncheckedIOException(erro); }
	}
	@Override public void apagar(String chave) { try { Files.deleteIfExists(caminho(chave)); } catch (IOException erro) { throw new UncheckedIOException(erro); } }
	@Override public int apagarAntigos(String prefixo,Duration idade) {
		var base=caminho(prefixo.endsWith("/")?prefixo.substring(0,prefixo.length()-1):prefixo);
		if(!Files.isDirectory(base))return 0;
		var limite=relogio.instant().minus(idade);int n=0;
		try(Stream<Path> arquivos=Files.walk(base)) {
			for(var p:(Iterable<Path>)arquivos.filter(Files::isRegularFile)::iterator)
				if(Files.getLastModifiedTime(p).toInstant().isBefore(limite)){Files.deleteIfExists(p);n++;}
		} catch (IOException erro) { throw new UncheckedIOException(erro); }
		return n;
	}
	@Override public Acesso urlAssinada(String chave,Duration validade,String nomeArquivo,String tipo) {
		caminho(chave);var expira=relogio.instant().plus(validade);
		var token=Base64.getUrlEncoder().withoutPadding().encodeToString(chave.getBytes(StandardCharsets.UTF_8));
		var nome=nomeArquivo==null?"":nomeArquivo;var t=tipo==null?"application/octet-stream":tipo;
		var carga=token+"|"+expira.getEpochSecond()+"|"+nome+"|"+t;
		return new Acesso(urlBase+"/api/v1/arquivos/"+token+"?exp="+expira.getEpochSecond()+"&nome="+URLEncoder.encode(nome,StandardCharsets.UTF_8)
			+"&tipo="+URLEncoder.encode(t,StandardCharsets.UTF_8)+"&sig="+assinar(carga),expira);
	}
	String assinar(String carga) {
		try { var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(segredo,"HmacSHA256"));return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(carga.getBytes(StandardCharsets.UTF_8))); }
		catch (java.security.GeneralSecurityException erro) { throw new IllegalStateException(erro); }
	}
	/** Valida a assinatura e a validade; devolve a chave do arquivo. */
	public Optional<String> validar(String token,long exp,String nome,String tipo,String sig) {
		if(relogio.instant().getEpochSecond()>exp||sig==null)return Optional.empty();
		var esperado=assinar(token+"|"+exp+"|"+nome+"|"+tipo);
		if(!MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),sig.getBytes(StandardCharsets.UTF_8)))return Optional.empty();
		try { return Optional.of(new String(Base64.getUrlDecoder().decode(token),StandardCharsets.UTF_8)); } catch (IllegalArgumentException erro) { return Optional.empty(); }
	}
}
