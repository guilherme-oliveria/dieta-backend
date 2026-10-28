package com.br.dietamais.assinaturas;

import com.br.dietamais.assinaturas.aplicacao.GatewayPagamento;
import java.util.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods=false)
public class GatewayTesteConfig {
	public static class GatewayFalso implements GatewayPagamento {
		public boolean ligado=true;public int criados=0;public final List<String> cancelados=new ArrayList<>();
		@Override public boolean disponivel(){return ligado;}
		@Override public CheckoutCriado criarCheckout(DadosCheckout d){criados++;return new CheckoutCriado("cus_"+criados,"sub_"+d.assinaturaId()+"_"+criados,"https://pagamento.invalid/checkout/"+criados);}
		@Override public Optional<String> urlCobranca(String id){return Optional.of("https://pagamento.invalid/cobranca/"+id);}
		@Override public void cancelar(String id){cancelados.add(id);}
		@Override public boolean tokenValido(String token){return "segredo-de-teste".equals(token);}
		/** Formato simples só para o teste: "id|TIPO|assinatura|vencimento". */
		@Override public Evento interpretar(String corpo){
			var p=corpo.split("\\|",-1);if(p.length!=4)throw new IllegalArgumentException("formato");
			return new Evento(p[0],TipoEvento.valueOf(p[1]),p[2].isEmpty()?null:p[2],p[3].isEmpty()?null:p[3]);
		}
	}
	@Bean @Primary GatewayFalso gatewayFalso(){return new GatewayFalso();}
}
