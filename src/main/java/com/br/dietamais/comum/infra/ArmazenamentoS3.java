package com.br.dietamais.comum.infra;

import com.br.dietamais.comum.api.Armazenamento;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * S3 (ou MinIO) com assinatura SigV4, sem o SDK da AWS. URL assinada para o download; PUT/GET/DELETE internos assinados por cabeçalho.
 * Estilo de caminho ({@code endpoint/bucket/chave}): funciona com MinIO e com S3.
 */
public class ArmazenamentoS3 implements Armazenamento {
	private static final DateTimeFormatter DATA_HORA=DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
	private static final DateTimeFormatter DATA=DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
	private final URI endpoint;private final String bucket;private final String regiao;private final String acesso;private final String segredo;private final Clock relogio;
	private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

	public ArmazenamentoS3(String endpoint,String bucket,String regiao,String acesso,String segredo,Clock relogio) {
		this.endpoint=URI.create(endpoint.endsWith("/")?endpoint.substring(0,endpoint.length()-1):endpoint);this.bucket=bucket;this.regiao=regiao;this.acesso=acesso;this.segredo=segredo;this.relogio=relogio;
	}

	private String host(){return endpoint.getPort()<0?endpoint.getHost():endpoint.getHost()+":"+endpoint.getPort();}
	private String caminho(String chave){return "/"+bucket+"/"+codificar(chave,false);}

	@Override public void guardar(String chave,byte[] conteudo,String tipo) {
		var r=enviar("PUT",chave,conteudo,tipo);
		if(r.statusCode()/100!=2)throw new IllegalStateException("Falha ao guardar o arquivo (HTTP "+r.statusCode()+").");
	}
	@Override public Optional<byte[]> ler(String chave) {
		var r=enviar("GET",chave,new byte[0],null);
		if(r.statusCode()==404)return Optional.empty();
		if(r.statusCode()/100!=2)throw new IllegalStateException("Falha ao ler o arquivo (HTTP "+r.statusCode()+").");
		return Optional.of(r.body());
	}
	@Override public void apagar(String chave){enviar("DELETE",chave,new byte[0],null);}
	@Override public int apagarAntigos(String prefixo,Duration idade){return 0;}

	private HttpResponse<byte[]> enviar(String metodo,String chave,byte[] corpo,String tipo) {
		try {
			var agora=relogio.instant();var data=DATA_HORA.format(agora);var hash=hex(sha256(corpo));
			var cab=new TreeMap<String,String>();cab.put("host",host());cab.put("x-amz-content-sha256",hash);cab.put("x-amz-date",data);
			if(tipo!=null)cab.put("content-type",tipo);
			var canonico=metodo+"\n"+caminho(chave)+"\n\n"+cab.entrySet().stream().map(e->e.getKey()+":"+e.getValue()+"\n").reduce("",String::concat)+"\n"+String.join(";",cab.keySet())+"\n"+hash;
			var escopo=DATA.format(agora)+"/"+regiao+"/s3/aws4_request";
			var texto="AWS4-HMAC-SHA256\n"+data+"\n"+escopo+"\n"+hex(sha256(canonico.getBytes(StandardCharsets.UTF_8)));
			var assinatura=hex(hmac(chaveAssinatura(DATA.format(agora)),texto));
			var pedido=HttpRequest.newBuilder(URI.create(endpoint.getScheme()+"://"+host()+caminho(chave))).timeout(Duration.ofSeconds(30))
				.method(metodo,corpo.length==0&&!metodo.equals("PUT")?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(corpo))
				.header("x-amz-content-sha256",hash).header("x-amz-date",data)
				.header("Authorization","AWS4-HMAC-SHA256 Credential="+acesso+"/"+escopo+", SignedHeaders="+String.join(";",cab.keySet())+", Signature="+assinatura);
			if(tipo!=null)pedido.header("Content-Type",tipo);
			return http.send(pedido.build(),HttpResponse.BodyHandlers.ofByteArray());
		} catch (java.io.IOException erro) { throw new java.io.UncheckedIOException(erro); }
		catch (InterruptedException erro) { Thread.currentThread().interrupt();throw new IllegalStateException("Interrompido."); }
	}

	@Override public Acesso urlAssinada(String chave,Duration validade,String nomeArquivo,String tipo) {
		var agora=relogio.instant();
		var disposicao=nomeArquivo==null?null:"attachment; filename=\""+nomeArquivo.replace("\"","")+"\"";
		var url=presign("GET",host(),caminho(chave),agora,validade.toSeconds(),disposicao,tipo);
		return new Acesso(endpoint.getScheme()+"://"+host()+url,agora.plus(validade));
	}

	/** SigV4 por query string. Devolve {@code caminho?parâmetros-assinados}. Aberto ao pacote para o teste com o vetor da documentação da AWS. */
	String presign(String metodo,String host,String caminho,Instant agora,long expiraSegundos,String disposicao,String tipo) {
		var data=DATA_HORA.format(agora);var escopo=DATA.format(agora)+"/"+regiao+"/s3/aws4_request";
		var q=new TreeMap<String,String>();
		q.put("X-Amz-Algorithm","AWS4-HMAC-SHA256");q.put("X-Amz-Credential",acesso+"/"+escopo);q.put("X-Amz-Date",data);
		q.put("X-Amz-Expires",Long.toString(expiraSegundos));q.put("X-Amz-SignedHeaders","host");
		if(disposicao!=null)q.put("response-content-disposition",disposicao);
		if(tipo!=null)q.put("response-content-type",tipo);
		var consulta=q.entrySet().stream().map(e->codificar(e.getKey(),true)+"="+codificar(e.getValue(),true)).reduce((a,b)->a+"&"+b).orElse("");
		var canonico=metodo+"\n"+caminho+"\n"+consulta+"\nhost:"+host+"\n\nhost\nUNSIGNED-PAYLOAD";
		var texto="AWS4-HMAC-SHA256\n"+data+"\n"+escopo+"\n"+hex(sha256(canonico.getBytes(StandardCharsets.UTF_8)));
		return caminho+"?"+consulta+"&X-Amz-Signature="+hex(hmac(chaveAssinatura(DATA.format(agora)),texto));
	}

	private byte[] chaveAssinatura(String dia) {
		var k=hmac(("AWS4"+segredo).getBytes(StandardCharsets.UTF_8),dia);k=hmac(k,regiao);k=hmac(k,"s3");return hmac(k,"aws4_request");
	}
	private static byte[] hmac(byte[] chave,String texto) {
		try { var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(chave,"HmacSHA256"));return mac.doFinal(texto.getBytes(StandardCharsets.UTF_8)); }
		catch (java.security.GeneralSecurityException erro) { throw new IllegalStateException(erro); }
	}
	private static byte[] sha256(byte[] dados){try{return MessageDigest.getInstance("SHA-256").digest(dados);}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
	private static String hex(byte[] b){var s=new StringBuilder();for(var x:b)s.append(String.format("%02x",x));return s.toString();}
	/** Codificação do SigV4: só A-Z a-z 0-9 - _ . ~ ficam; a barra fica quando é caminho. */
	static String codificar(String texto,boolean barra) {
		var r=URLEncoder.encode(texto,StandardCharsets.UTF_8).replace("+","%20").replace("*","%2A").replace("%7E","~");
		return barra?r:r.replace("%2F","/");
	}
}
