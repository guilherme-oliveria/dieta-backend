package com.br.dietamais;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArquiteturaTest {
	@Test
	void modulosNaoTemCiclosEAcessamSomenteApi() {
		var modulos = ApplicationModules.of(DietamaisApplication.class);
		modulos.verify();
		assertThat(modulos.stream().map(modulo -> modulo.getIdentifier().toString()))
				.contains("comum", "contas", "pacientes", "assinaturas", "sync", "dietas", "alimentos",
						"rotina", "avaliacoes", "questionarios", "agenda", "notificacoes", "relatorios", "admin");
	}
}
