package com.br.dietamais.contas.infra;

import com.br.dietamais.comum.api.Perfil;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.time.Clock;
import java.util.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

public class ChavesJwt {
	private final RSAPrivateKey privada;
	private final RSAPublicKey publica;
	public ChavesJwt(String privadaPem, String publicaPem, boolean desenvolvimento) {
		try {
			if (privadaPem.isBlank() && publicaPem.isBlank() && desenvolvimento) {
				var gerador = KeyPairGenerator.getInstance("RSA"); gerador.initialize(2048);
				var par = gerador.generateKeyPair();
				privada = (RSAPrivateKey) par.getPrivate(); publica = (RSAPublicKey) par.getPublic();
			} else {
				if (privadaPem.isBlank() || publicaPem.isBlank()) throw new GeneralSecurityException();
				var fabrica = KeyFactory.getInstance("RSA");
				privada = (RSAPrivateKey) fabrica.generatePrivate(new PKCS8EncodedKeySpec(pem(privadaPem, "PRIVATE KEY")));
				publica = (RSAPublicKey) fabrica.generatePublic(new X509EncodedKeySpec(pem(publicaPem, "PUBLIC KEY")));
			}
			if (publica.getModulus().bitLength() < 2048 || !privada.getModulus().equals(publica.getModulus())) throw new GeneralSecurityException();
		} catch (GeneralSecurityException | IllegalArgumentException erro) {
			throw new IllegalStateException("Configure um par válido de chaves RSA para JWT; geração efêmera só em dev/test.");
		}
	}
	private static byte[] pem(String valor, String tipo) {
		return Base64.getDecoder().decode(valor.replace("\\n", "\n").replace("-----BEGIN " + tipo + "-----", "")
				.replace("-----END " + tipo + "-----", "").replaceAll("\\s", ""));
	}
	public JwtEncoder encoder() {
		var chave = new RSAKey.Builder(publica).privateKey(privada).keyID("dietamais-rs256").build();
		return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(chave)));
	}
	public JwtDecoder decoder(Clock relogio) {
		var decoder = NimbusJwtDecoder.withPublicKey(publica).signatureAlgorithm(SignatureAlgorithm.RS256).build();
		decoder.setJwtValidator(jwt -> {
			try {
				var perfil = Perfil.valueOf(jwt.getClaimAsString("perfil"));
				UUID.fromString(jwt.getSubject());
				if (perfil == Perfil.NUTRICIONISTA) UUID.fromString(jwt.getClaimAsString("nid"));
				if (perfil == Perfil.PACIENTE) UUID.fromString(jwt.getClaimAsString("pid"));
				if (!"dietamais".equals(jwt.getClaimAsString("iss")) || !jwt.getAudience().contains("dietamais-api")
						|| jwt.getExpiresAt() == null || !relogio.instant().isBefore(jwt.getExpiresAt())
						|| jwt.getIssuedAt() == null || jwt.getIssuedAt().isAfter(relogio.instant())
						|| (jwt.getNotBefore() != null && jwt.getNotBefore().isAfter(relogio.instant()))) throw new IllegalArgumentException();
				return OAuth2TokenValidatorResult.success();
			} catch (RuntimeException erro) {
				return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Token inválido ou expirado.", null));
			}
		});
		return decoder;
	}
}
