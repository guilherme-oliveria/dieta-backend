package com.br.dietamais.assinaturas;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import com.br.dietamais.assinaturas.aplicacao.GatewayPagamento;
import com.br.dietamais.assinaturas.infra.GatewayAsaas;
import com.br.dietamais.comum.api.ExcecaoNegocio;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class GatewayAsaasTest {
	final Clock relogio=Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"),ZoneOffset.UTC);
	final JsonMapper mapper=JsonMapper.builder().build();

	record Montagem(GatewayAsaas gateway,MockRestServiceServer servidor) { }
	Montagem montar(){
		var construtor=RestClient.builder();var servidor=MockRestServiceServer.bindTo(construtor).build();
		return new Montagem(new GatewayAsaas(construtor,mapper,relogio,"https://asaas.invalid","chave-de-teste","token-de-teste"),servidor);
	}

	@Test void checkoutCriaClienteAssinaturaEDevolveLinkDaCobranca() {
		var m=montar();
		m.servidor().expect(requestTo("https://asaas.invalid/v3/customers")).andExpect(header("access_token","chave-de-teste")).andExpect(header("User-Agent","DietaMais"))
			.andRespond(withSuccess("{\"id\":\"cus_1\"}",MediaType.APPLICATION_JSON));
		m.servidor().expect(requestTo("https://asaas.invalid/v3/subscriptions")).andExpect(jsonPath("$.customer").value("cus_1")).andExpect(jsonPath("$.cycle").value("MONTHLY"))
			.andExpect(jsonPath("$.billingType").value("UNDEFINED")).andExpect(jsonPath("$.nextDueDate").value("2026-10-29")).andExpect(jsonPath("$.value").value(29.9))
			.andRespond(withSuccess("{\"id\":\"sub_1\"}",MediaType.APPLICATION_JSON));
		m.servidor().expect(requestTo("https://asaas.invalid/v3/subscriptions/sub_1/payments"))
			.andRespond(withSuccess("{\"data\":[{\"invoiceUrl\":\"https://asaas.invalid/i/abc\"}]}",MediaType.APPLICATION_JSON));
		var r=m.gateway().criarCheckout(new GatewayPagamento.DadosCheckout(UUID.randomUUID(),"Nutri","n@example.invalid","BASICO_20","Básico",new BigDecimal("29.90"),null));
		assertThat(r.url()).isEqualTo("https://asaas.invalid/i/abc");assertThat(r.assinaturaGatewayId()).isEqualTo("sub_1");assertThat(r.clienteId()).isEqualTo("cus_1");
		m.servidor().verify();
	}

	@Test void falhaDoGatewayViraPagamentoIndisponivelSemVazarDetalhe() {
		var m=montar();
		m.servidor().expect(requestTo("https://asaas.invalid/v3/customers")).andRespond(withServerError());
		assertThatThrownBy(()->m.gateway().criarCheckout(new GatewayPagamento.DadosCheckout(UUID.randomUUID(),"N","n@example.invalid","B","B",BigDecimal.TEN,null)))
			.isInstanceOf(ExcecaoNegocio.class).extracting(e->((ExcecaoNegocio)e).codigo().name()).isEqualTo("PAGAMENTO_INDISPONIVEL");
	}

	@Test void interpretaEventosDoWebhook() {
		var g=montar().gateway();
		var c=g.interpretar("{\"id\":\"evt_1\",\"event\":\"PAYMENT_CONFIRMED\",\"payment\":{\"id\":\"pay_1\",\"subscription\":\"sub_1\",\"dueDate\":\"2026-11-10\"}}");
		assertThat(c.tipo()).isEqualTo(GatewayPagamento.TipoEvento.PAGAMENTO_CONFIRMADO);assertThat(c.assinaturaGatewayId()).isEqualTo("sub_1");
		assertThat(c.vencimento()).isEqualTo("2026-11-10");assertThat(c.eventoId()).isEqualTo("evt_1");
		assertThat(g.interpretar("{\"event\":\"PAYMENT_OVERDUE\",\"payment\":{\"id\":\"pay_2\",\"subscription\":\"sub_1\",\"dueDate\":\"2026-11-10\"}}").eventoId()).isEqualTo("PAYMENT_OVERDUE:pay_2");
		var d=g.interpretar("{\"id\":\"evt_3\",\"event\":\"SUBSCRIPTION_DELETED\",\"subscription\":{\"id\":\"sub_1\"}}");
		assertThat(d.tipo()).isEqualTo(GatewayPagamento.TipoEvento.ASSINATURA_CANCELADA);assertThat(d.assinaturaGatewayId()).isEqualTo("sub_1");
		assertThat(g.interpretar("{\"id\":\"evt_4\",\"event\":\"PAYMENT_CREATED\",\"payment\":{\"id\":\"p\"}}").tipo()).isEqualTo(GatewayPagamento.TipoEvento.IGNORADO);
		assertThatThrownBy(()->g.interpretar("não é json")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(()->g.interpretar("{}")).isInstanceOf(IllegalArgumentException.class);
		assertThat(g.tokenValido("token-de-teste")).isTrue();assertThat(g.tokenValido("outro")).isFalse();assertThat(g.tokenValido(null)).isFalse();
	}
}
