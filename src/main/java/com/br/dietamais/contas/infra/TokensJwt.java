package com.br.dietamais.contas.infra;

import com.br.dietamais.comum.api.Perfil;
import java.time.*;
import java.util.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class TokensJwt {
	private final JwtEncoder encoder;
	private final Clock relogio;
	public TokensJwt(JwtEncoder encoder, Clock relogio) { this.encoder = encoder; this.relogio = relogio; }
	public Emitido emitir(UUID usuario, Perfil perfil, UUID nid, UUID pid) {
		var agora = relogio.instant(); var fim = agora.plus(Duration.ofMinutes(15));
		var claims = JwtClaimsSet.builder().issuer("dietamais").audience(List.of("dietamais-api"))
				.subject(usuario.toString()).issuedAt(agora).notBefore(agora).expiresAt(fim).claim("perfil", perfil.name());
		if (perfil == Perfil.NUTRICIONISTA) claims.claim("nid", Objects.requireNonNull(nid).toString());
		if (perfil == Perfil.PACIENTE) claims.claim("pid", Objects.requireNonNull(pid).toString());
		String valor = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims.build())).getTokenValue();
		return new Emitido(valor, fim);
	}
	public record Emitido(String valor, Instant expiraEm) {
		@Override public String toString() { return "Emitido[token protegido]"; }
	}
}
