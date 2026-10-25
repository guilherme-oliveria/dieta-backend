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

	// Cadastro é público; as demais rotas seguem protegidas até o login com JWT em BE-03.
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, FabricaProblema problemas) throws Exception {
		org.springframework.security.web.AuthenticationEntryPoint entrada = (requisicao, resposta, erro) -> {
			resposta.setHeader("WWW-Authenticate", "Basic realm=\"Dieta+\", charset=\"UTF-8\"");
			problemas.escrever(resposta, CodigoErro.NAO_AUTENTICADO, "Autenticação necessária.");
		};
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health/**").permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/auth/cadastro-nutri").permitAll()
						.anyRequest().authenticated())
				.httpBasic(basic -> basic.authenticationEntryPoint(entrada))
				.exceptionHandling(erros -> erros
						.authenticationEntryPoint(entrada)
						.accessDeniedHandler((requisicao, resposta, erro) -> problemas.escrever(resposta,
								CodigoErro.ACESSO_NEGADO, "Acesso não permitido.")))
				.build();
	}

}
