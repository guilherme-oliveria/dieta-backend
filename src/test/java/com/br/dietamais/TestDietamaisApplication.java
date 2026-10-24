package com.br.dietamais;

import org.springframework.boot.SpringApplication;

// Sobe a aplicação com um Postgres 18 via Testcontainers, sem precisar do compose.yaml
public class TestDietamaisApplication {

	public static void main(String[] args) {
		SpringApplication.from(DietamaisApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
