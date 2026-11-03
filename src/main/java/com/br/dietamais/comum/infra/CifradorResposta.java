package com.br.dietamais.comum.infra;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CifradorResposta implements com.br.dietamais.comum.api.AutenticadorConteudo {
	private static final byte VERSAO = 1;
	private static final int NONCE_BYTES = 12;
	private final SecretKey chave;
	private final SecureRandom aleatorio = new SecureRandom();

	public CifradorResposta(@Value("${IDEMPOTENCIA_CHAVE:}") String chaveBase64) {
		byte[] bytes = chaveBase64.isBlank() ? null : Base64.getDecoder().decode(chaveBase64);
		if (bytes != null && bytes.length != 32) throw new IllegalArgumentException("IDEMPOTENCIA_CHAVE deve conter 32 bytes em Base64");
		chave = bytes == null ? null : new SecretKeySpec(bytes, "AES");
	}

	public byte[] cifrar(byte[] corpo, String contexto) {
		byte[] nonce = new byte[NONCE_BYTES];
		aleatorio.nextBytes(nonce);
		byte[] cifrado = transformar(Cipher.ENCRYPT_MODE, corpo, nonce, contexto);
		return ByteBuffer.allocate(1 + nonce.length + cifrado.length).put(VERSAO).put(nonce).put(cifrado).array();
	}

	public Optional<String> autenticarRequisicao(byte[] dados) {
		if (chave == null) return Optional.empty();
		try {
			var mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(chave.getEncoded(), "HmacSHA256"));
			byte[] derivada = mac.doFinal("Dieta+/idempotencia/hash-requisicao/v1".getBytes(StandardCharsets.UTF_8));
			mac.init(new SecretKeySpec(derivada, "HmacSHA256"));
			return Optional.of(HexFormat.of().formatHex(mac.doFinal(dados)));
		} catch (GeneralSecurityException erro) { throw new IllegalStateException("Não foi possível autenticar a requisição", erro); }
	}

	public byte[] decifrar(byte[] corpo, String contexto) {
		if (corpo.length < 1 + NONCE_BYTES + 16 || corpo[0] != VERSAO) throw new IllegalStateException("Resposta idempotente inválida");
		var buffer = ByteBuffer.wrap(corpo);
		buffer.get();
		byte[] nonce = new byte[NONCE_BYTES];
		buffer.get(nonce);
		byte[] cifrado = new byte[buffer.remaining()];
		buffer.get(cifrado);
		return transformar(Cipher.DECRYPT_MODE, cifrado, nonce, contexto);
	}

	private byte[] transformar(int modo, byte[] dados, byte[] nonce, String contexto) {
		if (chave == null) throw new IllegalStateException("Configure IDEMPOTENCIA_CHAVE antes de usar operações idempotentes");
		try {
			var cifra = Cipher.getInstance("AES/GCM/NoPadding");
			cifra.init(modo, chave, new GCMParameterSpec(128, nonce));
			cifra.updateAAD(contexto.getBytes(StandardCharsets.UTF_8));
			return cifra.doFinal(dados);
		} catch (GeneralSecurityException erro) { throw new IllegalStateException("Não foi possível acessar a resposta idempotente", erro); }
	}
}
