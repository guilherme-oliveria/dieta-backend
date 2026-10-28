package com.br.dietamais.assinaturas.infra;

import com.br.dietamais.assinaturas.aplicacao.GatewayPagamento;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Configuration
class PagamentoConfig {
	@Bean GatewayPagamento gatewayPagamento(JsonMapper mapper,Clock relogio,
			@Value("${dieta.pagamento.asaas.api-key:}") String chave,
			@Value("${dieta.pagamento.asaas.url-base:https://api-sandbox.asaas.com}") String urlBase,
			@Value("${dieta.pagamento.asaas.token-webhook:}") String token) {
		return chave.isBlank()?new GatewayDesligado():new GatewayAsaas(RestClient.builder(),mapper,relogio,urlBase,chave,token);
	}
}
