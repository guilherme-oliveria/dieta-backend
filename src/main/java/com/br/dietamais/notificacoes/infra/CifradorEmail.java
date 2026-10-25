package com.br.dietamais.notificacoes.infra;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CifradorEmail {
	private final SecretKeySpec chave;
	private final SecureRandom aleatorio = new SecureRandom();
	public CifradorEmail(@Value("${NOTIFICACOES_CHAVE:}") String configuracao) {
		try {
			byte[] bytes = configuracao.isBlank() ? null : Base64.getDecoder().decode(configuracao);
			if (bytes != null && bytes.length != 32) throw new IllegalArgumentException();
			chave = bytes == null ? null : new SecretKeySpec(bytes, "AES");
		} catch (IllegalArgumentException erro) { throw new IllegalStateException("NOTIFICACOES_CHAVE precisa conter 32 bytes em Base64."); }
	}
	public String cifrar(UUID id, String tipo, String texto) {
		try {
			byte[] nonce = new byte[12]; aleatorio.nextBytes(nonce);
			byte[] cifrado = preparar(Cipher.ENCRYPT_MODE,id,tipo,nonce).doFinal(texto.getBytes(StandardCharsets.UTF_8));
			return "v1." + Base64.getEncoder().encodeToString(nonce) + "." + Base64.getEncoder().encodeToString(cifrado);
		} catch (GeneralSecurityException erro) { throw new IllegalStateException("Não foi possível proteger a notificação."); }
	}
	public String decifrar(UUID id, String tipo, String valor) {
		try {
			var partes = valor.split("\\.");
			if (partes.length != 3 || !partes[0].equals("v1")) throw new IllegalArgumentException();
			byte[] nonce = Base64.getDecoder().decode(partes[1]);
			if (nonce.length != 12) throw new IllegalArgumentException();
			return new String(preparar(Cipher.DECRYPT_MODE,id,tipo,nonce).doFinal(Base64.getDecoder().decode(partes[2])),StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | IllegalArgumentException erro) { throw new IllegalStateException("Não foi possível abrir a notificação."); }
	}
	private Cipher preparar(int modo, UUID id, String tipo, byte[] nonce) throws GeneralSecurityException {
		if (chave == null) throw new IllegalStateException("Configure NOTIFICACOES_CHAVE antes de enfileirar e-mails.");
		var cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(modo,chave,new GCMParameterSpec(128,nonce));
		cipher.updateAAD(("Dieta+/email/v1/" + id + "/" + tipo).getBytes(StandardCharsets.UTF_8));
		return cipher;
	}
}
