package com.br.dietamais.contas.infra;

import static org.assertj.core.api.Assertions.*;
import com.br.dietamais.comum.api.Perfil;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

class TokensJwtTest {
	private final Clock relogio = Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC);
	@Test void assinaRs256ComIdentidadeEQuinzeMinutosDeValidade() {
		var chaves = new ChavesJwt("", "", true);
		var tokens = new TokensJwt(chaves.encoder(), relogio);
		var id = UUID.randomUUID(); var nid = UUID.randomUUID();
		var emitido = tokens.emitir(id, Perfil.NUTRICIONISTA, nid, null);
		var jwt = chaves.decoder(relogio).decode(emitido.valor());
		assertThat(jwt.getHeaders().get("alg")).isEqualTo("RS256");
		assertThat(jwt.getSubject()).isEqualTo(id.toString());
		assertThat(jwt.getClaimAsString("perfil")).isEqualTo("NUTRICIONISTA");
		assertThat(jwt.getClaimAsString("nid")).isEqualTo(nid.toString());
		assertThat(jwt.hasClaim("pid")).isFalse();
		assertThat(jwt.getExpiresAt()).isEqualTo(relogio.instant().plusSeconds(900));
		assertThat(emitido.expiraEm()).isEqualTo(jwt.getExpiresAt());
	}
	@Test void rejeitaAssinaturaAlheiaEExpiracaoNoLimite() {
		var chaves = new ChavesJwt("", "", true);
		var emitido = new TokensJwt(chaves.encoder(), relogio).emitir(UUID.randomUUID(), Perfil.ADMIN, null, null);
		assertThatThrownBy(() -> new ChavesJwt("", "", true).decoder(relogio).decode(emitido.valor())).isInstanceOf(JwtException.class);
		assertThatThrownBy(() -> chaves.decoder(Clock.offset(relogio, Duration.ofMinutes(15))).decode(emitido.valor())).isInstanceOf(JwtException.class);
	}
	@Test void semChavesEmProducaoOuParIncompletoRecusaIniciar() {
		assertThatThrownBy(() -> new ChavesJwt("", "", false)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new ChavesJwt("PEM_INVALIDO", "", true)).isInstanceOf(IllegalStateException.class);
	}
	@Test void aceitaPemPorAmbienteERecusaChavesFracasOuDeParesDiferentes() throws Exception {
		var gerador = java.security.KeyPairGenerator.getInstance("RSA"); gerador.initialize(2048);
		var par = gerador.generateKeyPair(); var outro = gerador.generateKeyPair();
		String privada = pem("PRIVATE KEY", par.getPrivate().getEncoded());
		String publica = pem("PUBLIC KEY", par.getPublic().getEncoded());
		var chaves = new ChavesJwt(privada.replace("\n", "\\n"), publica, false);
		var emitido = new TokensJwt(chaves.encoder(), relogio).emitir(UUID.randomUUID(), Perfil.ADMIN, null, null);
		assertThat(chaves.decoder(relogio).decode(emitido.valor()).getClaimAsString("perfil")).isEqualTo("ADMIN");
		assertThatThrownBy(() -> new ChavesJwt(privada,pem("PUBLIC KEY",outro.getPublic().getEncoded()),false)).isInstanceOf(IllegalStateException.class);
		gerador.initialize(1024); var fraco = gerador.generateKeyPair();
		assertThatThrownBy(() -> new ChavesJwt(pem("PRIVATE KEY",fraco.getPrivate().getEncoded()),pem("PUBLIC KEY",fraco.getPublic().getEncoded()),false))
				.isInstanceOf(IllegalStateException.class);
	}
	private String pem(String tipo, byte[] bytes) {
		return "-----BEGIN " + tipo + "-----\n" + java.util.Base64.getEncoder().encodeToString(bytes) + "\n-----END " + tipo + "-----";
	}
}
