package com.br.dietamais.relatorios.aplicacao;

import com.br.dietamais.comum.api.Armazenamento;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Apaga os PDFs gerados depois que o link expirou: dado de saúde não fica guardado. */
@Component
public class LimpezaPdf {
	public static final String PREFIXO="pdf";
	private final Armazenamento armazenamento;
	public LimpezaPdf(Armazenamento armazenamento){this.armazenamento=armazenamento;}
	@Scheduled(fixedDelayString="PT5M",initialDelayString="PT1M")
	public void apagarVencidos(){armazenamento.apagarAntigos(PREFIXO,Duration.ofMinutes(30));}
}
