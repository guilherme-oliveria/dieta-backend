package com.br.dietamais.contas.infra;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import com.br.dietamais.contas.dominio.PoliticaSenha;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class SegurancaContasConfig {
	@Bean
	PasswordEncoder codificadorSenha() {
		return new DelegatingPasswordEncoder("argon2", Map.of("argon2", new Argon2PasswordEncoder(16, 32, 1, 19456, 2)));
	}

	@Bean
	PoliticaSenha politicaSenha(@Value("${dieta.contas.senhas-bloqueadas:classpath:seguranca/senhas-comuns-sha256.txt}") Resource recurso)
			throws java.io.IOException {
		try (var leitor = new BufferedReader(new InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8))) {
			var hashes = leitor.lines().map(String::strip).filter(linha -> !linha.isEmpty() && !linha.startsWith("#"))
					.collect(Collectors.toUnmodifiableSet());
			if (hashes.isEmpty() || hashes.stream().anyMatch(hash -> !hash.matches("[a-f0-9]{64}"))) {
				throw new IllegalStateException("Lista de senhas comuns inválida");
			}
			return new PoliticaSenha(hashes);
		}
	}
}
