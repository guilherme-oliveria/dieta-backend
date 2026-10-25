package com.br.dietamais.comum.infra;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@org.springframework.scheduling.annotation.EnableScheduling
public class RelogioConfig {
	@Bean
	Clock relogio() { return Clock.systemUTC(); }
}
