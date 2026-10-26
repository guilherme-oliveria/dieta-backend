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
	SecurityFilterChain securityFilterChain(HttpSecurity http, FabricaProblema problemas) throws Exception {
		org.springframework.security.web.AuthenticationEntryPoint entrada = (requisicao, resposta, erro) -> {
			resposta.setHeader("WWW-Authenticate", "Bearer");
			problemas.escrever(resposta, CodigoErro.NAO_AUTENTICADO, "Autenticação necessária.");
		};
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health/**").permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.GET,"/api/v1/convites/*").permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.POST,"/api/v1/convites/*/aceitar").permitAll()
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

}
