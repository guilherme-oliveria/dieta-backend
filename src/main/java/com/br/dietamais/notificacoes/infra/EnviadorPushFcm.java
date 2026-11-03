package com.br.dietamais.notificacoes.infra;

import com.br.dietamais.notificacoes.api.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.*;
import java.util.Base64;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * FCM HTTP v1. A credencial é o JSON da conta de serviço, só em variável de ambiente ({@code FCM_CREDENCIAIS_JSON}).
 * Nunca registra token, corpo nem resposta do FCM.
 */
@Component
@ConditionalOnExpression("!'${dieta.push.fcm.credenciais-json:}'.isEmpty()")
public class EnviadorPushFcm implements EnviadorPush {
	private static final Duration LIMITE=Duration.ofSeconds(10);
	private final JsonMapper mapper;private final Clock relogio;private final HttpClient http;
	private final String projeto;private final String email;private final java.security.PrivateKey chave;private final String urlToken;private final String urlEnvio;
	private String acesso;private Instant acessoExpira=Instant.EPOCH;
	public EnviadorPushFcm(JsonMapper mapper,Clock relogio,@Value("${dieta.push.fcm.credenciais-json}") String credenciais,
			@Value("${dieta.push.fcm.url-base:https://fcm.googleapis.com}") String urlBase) {
		this.mapper=mapper;this.relogio=relogio;this.http=HttpClient.newBuilder().connectTimeout(LIMITE).build();
		try {
			JsonNode c=mapper.readTree(credenciais);
			projeto=c.get("project_id").asString();email=c.get("client_email").asString();
			urlToken=c.has("token_uri")?c.get("token_uri").asString():"https://oauth2.googleapis.com/token";
			var pem=c.get("private_key").asString().replaceAll("-----[A-Z ]+-----","").replaceAll("\\s","");
			chave=KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
		} catch (Exception erro) { throw new IllegalStateException("Credencial do FCM inválida."); }
		this.urlEnvio=urlBase+"/v1/projects/"+projeto+"/messages:send";
	}
	@Override public ResultadoPush enviar(MensagemPush m) {
		try {
			var corpo=mapper.writeValueAsString(Map.of("message",Map.of(
				"token",m.token(),
				"notification",Map.of("title",m.titulo(),"body",m.corpo()),
				"data",Map.of("tipo",m.tipo(),"sync","true","rota",m.rota()==null?"":m.rota()),
				"android",Map.of("priority","HIGH"),
				"apns",Map.of("payload",Map.of("aps",Map.of("content-available",1))))));
			var resposta=http.send(HttpRequest.newBuilder(URI.create(urlEnvio)).timeout(LIMITE)
				.header("Authorization","Bearer "+tokenDeAcesso()).header("Content-Type","application/json; charset=utf-8")
				.POST(HttpRequest.BodyPublishers.ofString(corpo)).build(),HttpResponse.BodyHandlers.ofString());
			int status=resposta.statusCode();
			if(status/100==2)return ResultadoPush.ENVIADO;
			if(status==401)invalidarAcesso();
			if(status==404||(status==400&&resposta.body().contains("UNREGISTERED")))return ResultadoPush.TOKEN_INVALIDO;
			LoggerFactory.getLogger(EnviadorPushFcm.class).warn("FCM recusou o envio; status={}",status);
			return ResultadoPush.FALHA_TEMPORARIA;
		} catch (InterruptedException erro) { Thread.currentThread().interrupt();return ResultadoPush.FALHA_TEMPORARIA; }
		catch (Exception erro) {
			LoggerFactory.getLogger(EnviadorPushFcm.class).warn("Falha técnica no envio de push; tipo={}",erro.getClass().getSimpleName());
			return ResultadoPush.FALHA_TEMPORARIA;
		}
	}
	private synchronized String tokenDeAcesso() throws Exception {
		var agora=relogio.instant();
		if(acesso!=null&&agora.isBefore(acessoExpira.minusSeconds(60)))return acesso;
		var cab=b64("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
		var carga=b64(mapper.writeValueAsString(Map.of("iss",email,"scope","https://www.googleapis.com/auth/firebase.messaging","aud",urlToken,
			"iat",agora.getEpochSecond(),"exp",agora.getEpochSecond()+3600)));
		var assinatura=Signature.getInstance("SHA256withRSA");assinatura.initSign(chave);assinatura.update((cab+"."+carga).getBytes(StandardCharsets.US_ASCII));
		var jwt=cab+"."+carga+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(assinatura.sign());
		var corpo="grant_type="+java.net.URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer",StandardCharsets.UTF_8)+"&assertion="+jwt;
		var r=http.send(HttpRequest.newBuilder(URI.create(urlToken)).timeout(LIMITE).header("Content-Type","application/x-www-form-urlencoded")
			.POST(HttpRequest.BodyPublishers.ofString(corpo)).build(),HttpResponse.BodyHandlers.ofString());
		if(r.statusCode()/100!=2)throw new IllegalStateException("OAuth do FCM recusado: "+r.statusCode());
		var json=mapper.readTree(r.body());
		acesso=json.get("access_token").asString();acessoExpira=agora.plusSeconds(json.has("expires_in")?json.get("expires_in").asLong():3600);
		return acesso;
	}
	private synchronized void invalidarAcesso(){acesso=null;}
	private static String b64(String texto){return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));}
}
