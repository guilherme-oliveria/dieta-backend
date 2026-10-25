package com.br.dietamais.contas.infra;

import java.time.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LimpezaSessoes {
	private final SessaoStore store;
	private final Clock relogio;
	public LimpezaSessoes(SessaoStore store,Clock relogio){this.store=store;this.relogio=relogio;}
	@Scheduled(fixedDelayString="${dieta.auth.intervalo-limpeza:PT1H}",initialDelayString="${dieta.auth.intervalo-limpeza:PT1H}")
	public void limpar(){var agora=relogio.instant();store.limparExpirados(agora.minus(Duration.ofDays(1)),agora);}
}
