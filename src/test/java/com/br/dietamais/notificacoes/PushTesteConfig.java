package com.br.dietamais.notificacoes;

import com.br.dietamais.notificacoes.api.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods=false)
public class PushTesteConfig {
	public static class EnviadorFalso implements EnviadorPush {
		public final List<MensagemPush> enviadas=new CopyOnWriteArrayList<>();
		public final Map<String,ResultadoPush> resultados=new ConcurrentHashMap<>();
		@Override public ResultadoPush enviar(MensagemPush m){
			var r=resultados.getOrDefault(m.token(),ResultadoPush.ENVIADO);
			if(r==ResultadoPush.ENVIADO)enviadas.add(m);
			return r;
		}
	}
	@Bean @Primary EnviadorFalso enviadorPushFalso(){return new EnviadorFalso();}
}
