package com.br.dietamais.assinaturas.dominio;

import com.br.dietamais.assinaturas.api.StatusAssinatura;
import java.time.*;

public class PoliticaEscritaAssinatura {
	private final Duration carencia;
	public PoliticaEscritaAssinatura(Duration carencia) {
		if(carencia.isNegative())throw new IllegalArgumentException("Carência inválida");
		this.carencia=carencia;
	}
	public boolean permite(StatusAssinatura status,Instant vencimento,Instant agora) {
		return switch(status) {
			case ATIVA -> true;
			case TESTE -> vencimento!=null&&agora.isBefore(vencimento);
			case EM_ATRASO -> vencimento!=null&&agora.isBefore(vencimento.plus(carencia));
			case CANCELADA,EXPIRADA -> false;
		};
	}
}
