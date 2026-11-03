package com.br.dietamais.contas.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public class LimiteTaxaLogin {
	private final int limite;
	private final int capacidade;
	private final Map<String, ArrayDeque<Instant>> acessos = new HashMap<>();
	public LimiteTaxaLogin(int limite, int capacidade) {
		if (limite < 1 || capacidade < 1) throw new IllegalArgumentException("Limite de taxa inválido");
		this.limite = limite;
		this.capacidade = capacidade;
	}
	public synchronized long espera(String ip, Instant agora) {
		var corte = agora.minusSeconds(60);
		acessos.values().forEach(fila -> { while (!fila.isEmpty() && !fila.peekFirst().isAfter(corte)) fila.removeFirst(); });
		acessos.values().removeIf(ArrayDeque::isEmpty);
		var fila = acessos.get(ip);
		if (fila == null) {
			if (acessos.size() >= capacidade) return 60;
			fila = new ArrayDeque<>();
			acessos.put(ip, fila);
		}
		if (fila.size() >= limite) return Math.max(1, (Duration.between(agora, fila.peekFirst().plusSeconds(60)).toMillis() + 999) / 1000);
		fila.addLast(agora);
		return 0;
	}
}
