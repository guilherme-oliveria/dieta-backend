package com.br.dietamais.comum.infra;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** Janela deslizante de 60 s por chave, com memória limitada: ao lotar, recusa chave nova em vez de crescer. */
final class JanelaTaxa {
	private final int limite;private final int capacidade;
	private final Map<String,ArrayDeque<Instant>> acessos=new HashMap<>();
	private int chamadas;
	JanelaTaxa(int limite,int capacidade){
		if(limite<1||capacidade<1)throw new IllegalArgumentException("Limite de taxa inválido");
		this.limite=limite;this.capacidade=capacidade;
	}
	/** 0 se a requisição passa; senão, segundos até poder tentar de novo. */
	synchronized long espera(String chave,Instant agora){
		var corte=agora.minusSeconds(60);
		if(++chamadas%1000==0)varrer(corte);
		var fila=acessos.get(chave);
		if(fila==null){
			if(acessos.size()>=capacidade){varrer(corte);if(acessos.size()>=capacidade)return 60;}
			fila=new ArrayDeque<>();acessos.put(chave,fila);
		}
		while(!fila.isEmpty()&&!fila.peekFirst().isAfter(corte))fila.removeFirst();
		if(fila.size()>=limite)return Math.max(1,(Duration.between(agora,fila.peekFirst().plusSeconds(60)).toMillis()+999)/1000);
		fila.addLast(agora);
		return 0;
	}
	private void varrer(Instant corte){
		acessos.values().forEach(f->{while(!f.isEmpty()&&!f.peekFirst().isAfter(corte))f.removeFirst();});
		acessos.values().removeIf(ArrayDeque::isEmpty);
	}
}
