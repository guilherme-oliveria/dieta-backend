package com.br.dietamais.pacientes.infra;

import com.br.dietamais.comum.api.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(-96)
public class FiltroTaxaConvite extends OncePerRequestFilter {
	private final FabricaProblema problemas;private final Clock relogio;private final int limite;private final Duration duracao;
	private final Map<String,Janela> ips=new HashMap<>();
	public FiltroTaxaConvite(FabricaProblema problemas,Clock relogio,@Value("${dieta.convite.limite-ip:60}") int limite,@Value("${dieta.convite.janela:PT1M}") Duration duracao) {
		if(limite<1||duracao.isNegative()||duracao.isZero())throw new IllegalArgumentException("Limite de convites inválido");
		this.problemas=problemas;this.relogio=relogio;this.limite=limite;this.duracao=duracao;
	}
	@Override protected void doFilterInternal(HttpServletRequest pedido,HttpServletResponse resposta,FilterChain cadeia) throws IOException,ServletException {
		if(pedido.getRequestURI().startsWith("/api/v1/convites/")) {
			long espera=consumir(pedido.getRemoteAddr());
			if(espera>0) {
				resposta.setHeader("Retry-After",Long.toString(espera));resposta.setHeader("Cache-Control","no-store");
				problemas.escrever(resposta,CodigoErro.TAXA_EXCEDIDA,"Aguarde antes de consultar outro convite.");return;
			}
		}
		cadeia.doFilter(pedido,resposta);
	}
	private synchronized long consumir(String ip) {
		var agora=relogio.instant();var janela=ips.get(ip);
		if(janela==null||!agora.isBefore(janela.fim())) {
			if(ips.size()>=10000){ips.entrySet().removeIf(item->!agora.isBefore(item.getValue().fim()));if(ips.size()>=10000&&!ips.containsKey(ip))return Math.max(1,duracao.toSeconds());}
			janela=new Janela(0,agora.plus(duracao));
		}
		if(janela.usos()>=limite)return Math.max(1,(Duration.between(agora,janela.fim()).toMillis()+999)/1000);
		ips.put(ip,new Janela(janela.usos()+1,janela.fim()));return 0;
	}
	private record Janela(int usos,Instant fim){}
}
