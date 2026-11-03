package com.br.dietamais.contas.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record BloqueioLogin(List<Instant> falhas, Instant bloqueadoAte) {
	private static final Duration JANELA = Duration.ofMinutes(15);
	public BloqueioLogin { falhas = List.copyOf(falhas); }
	public boolean bloqueado(Instant agora) { return bloqueadoAte != null && agora.isBefore(bloqueadoAte); }
	public BloqueioLogin falhar(Instant agora) {
		var recentes = new ArrayList<>(falhas.stream().filter(falha -> falha.isAfter(agora.minus(JANELA))).toList());
		recentes.add(agora);
		return new BloqueioLogin(recentes, recentes.size() >= 5 ? agora.plus(JANELA) : null);
	}
}
