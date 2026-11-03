package com.br.dietamais.contas.infra;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.oauth2.jwt.*;

@Configuration(proxyBeanMethods = false)
public class JwtConfig {
	@Bean ChavesJwt chavesJwt(@Value("${JWT_CHAVE_PRIVADA:}") String privada,
			@Value("${JWT_CHAVE_PUBLICA:}") String publica, Environment ambiente) {
		return new ChavesJwt(privada, publica, ambiente.acceptsProfiles(Profiles.of("dev", "test")));
	}
	@Bean JwtEncoder jwtEncoder(ChavesJwt chaves) { return chaves.encoder(); }
	@Bean JwtDecoder jwtDecoder(ChavesJwt chaves, Clock relogio) { return chaves.decoder(relogio); }
}
