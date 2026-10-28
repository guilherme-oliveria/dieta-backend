package com.br.dietamais.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import com.br.dietamais.comum.api.FabricaProblema;
import com.br.dietamais.comum.api.CodigoErro;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, FabricaProblema problemas,
			@org.springframework.beans.factory.annotation.Value("${dieta.seguranca.hsts:false}") boolean hsts,
			@org.springframework.beans.factory.annotation.Value("${dieta.seguranca.cors-origens:}") String corsOrigens) throws Exception {
		org.springframework.security.web.AuthenticationEntryPoint entrada = (requisicao, resposta, erro) -> {
			resposta.setHeader("WWW-Authenticate", "Bearer");
			problemas.escrever(resposta, CodigoErro.NAO_AUTENTICADO, "Autenticação necessária.");
		};
		return http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(fonteCors(corsOrigens)))
				.headers(cabecalhos -> {
					// API JSON: nada de conteúdo ativo, de frames nem de referrer (BE-31)
					cabecalhos.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"));
					cabecalhos.referrerPolicy(r -> r.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
					cabecalhos.frameOptions(f -> f.deny());
					// HSTS só quando o ambiente termina o TLS (atrás do Caddy, requisição chega com X-Forwarded-Proto=https)
					cabecalhos.httpStrictTransportSecurity(h -> {
						if (hsts) h.includeSubDomains(true).maxAgeInSeconds(31536000).requestMatcher(requisicao -> true); else h.disable();
					});
				})
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health/**").permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.GET,"/legal/terms-of-use").permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.GET,"/cal/*").permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.GET,"/api/v1/convites/*").permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.POST,"/api/v1/convites/*/aceitar").permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.POST,"/api/v1/webhooks/pagamento").permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.GET,"/api/v1/arquivos/*","/api/v1/exportacoes/*").permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/auth/cadastro-nutri",
								"/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/auth/confirmar-email",
								"/api/v1/auth/esqueci-senha", "/api/v1/auth/redefinir-senha").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth -> oauth
						.jwt(jwt -> jwt.jwtAuthenticationConverter(token -> new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
								token, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + token.getClaimAsString("perfil"))))))
						.authenticationEntryPoint(entrada))
				.exceptionHandling(erros -> erros
						.authenticationEntryPoint(entrada)
						.accessDeniedHandler((requisicao, resposta, erro) -> problemas.escrever(resposta,
								CodigoErro.ACESSO_NEGADO, "Acesso não permitido.")))
				.build();
	}

	/** Origens do site (CORS_ORIGENS, separadas por vírgula, HTTPS e exatas). Vazio: nenhuma origem de navegador é aceita. */
	static org.springframework.web.cors.CorsConfigurationSource fonteCors(String origens) {
		var fonte = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
		var lista = java.util.Arrays.stream(origens.split(",")).map(String::strip).filter(o -> !o.isEmpty()).toList();
		for (var origem : lista)
			if (!origem.matches("https://[A-Za-z0-9.-]+(:[0-9]{1,5})?") && !origem.matches("http://localhost(:[0-9]{1,5})?"))
				throw new IllegalStateException("CORS_ORIGENS aceita só origens HTTPS exatas (ou http://localhost em desenvolvimento).");
		if (lista.isEmpty()) return fonte;
		var cors = new org.springframework.web.cors.CorsConfiguration();
		cors.setAllowedOrigins(lista);
		cors.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "If-Match", "Idempotency-Key", "X-App-Versao"));
		cors.setExposedHeaders(java.util.List.of("ETag", "Retry-After", "Location"));
		cors.setAllowCredentials(true); // cookie HttpOnly do refresh no site; só vale com origem explícita
		cors.setMaxAge(3600L);
		fonte.registerCorsConfiguration("/api/**", cors);
		return fonte;
	}

}
