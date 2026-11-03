package com.br.dietamais.notificacoes.aplicacao;

import com.br.dietamais.notificacoes.infra.AvisoStore;
import java.time.*;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArquivarAvisos {
	static final Duration RETENCAO=Duration.ofDays(90);
	private final AvisoStore avisos;private final Clock relogio;
	public ArquivarAvisos(AvisoStore avisos,Clock relogio){this.avisos=avisos;this.relogio=relogio;}
	@Scheduled(fixedDelayString="${dieta.avisos.intervalo-arquivo:PT6H}",initialDelayString="${dieta.avisos.intervalo-arquivo:PT6H}")
	public void executarJob(){
		try { executar(); }
		catch (RuntimeException erro) { LoggerFactory.getLogger(ArquivarAvisos.class).warn("Falha técnica ao arquivar avisos."); }
	}
	@Transactional
	public int executar(){var agora=relogio.instant();return avisos.arquivarAnteriores(agora.minus(RETENCAO),agora);}
}
