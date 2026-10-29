package com.br.dietamais.contas.infra;

import com.br.dietamais.contas.dominio.DocumentoTermosUso;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

/**
 * Só para a carga de exemplo (perfil dev com {@code dieta.dev.dados-exemplo=true}): publica termos FICTÍCIOS para que o cadastro funcione na máquina do dev.
 * Não vale em produção (perfil prod ou sem a propriedade) e não substitui a aprovação real dos termos (RN-CON-09).
 */
@Configuration(proxyBeanMethods = false)
@Profile("dev")
@ConditionalOnProperty(name = "dieta.dev.dados-exemplo", havingValue = "true")
class TermosExemploDev {
	@Bean @Primary FonteTermosUso termosFicticiosDeDesenvolvimento() {
		return () -> new DocumentoTermosUso("2026-10", "PUBLICADO", Instant.EPOCH, Instant.EPOCH, "TERMOS FICTÍCIOS DE DESENVOLVIMENTO, SEM VALIDADE JURÍDICA.");
	}
}
