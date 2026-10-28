package com.br.dietamais.assinaturas.infra;

import com.br.dietamais.assinaturas.aplicacao.GatewayPagamento;
import com.br.dietamais.comum.api.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Optional;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Asaas API v3: cliente + assinatura mensal com forma de pagamento à escolha do pagador (checkout hospedado, a cobrança traz o link).
 * A chave só existe em variável de ambiente ({@code ASAAS_API_KEY}); nunca é registrada, nem o corpo das respostas.
 */
public class GatewayAsaas implements GatewayPagamento {
	private final RestClient http;private final JsonMapper mapper;private final Clock relogio;private final String tokenWebhook;
	public GatewayAsaas(RestClient.Builder construtor,JsonMapper mapper,Clock relogio,String urlBase,String chave,String tokenWebhook) {
		this.http=construtor.baseUrl(urlBase).defaultHeader("access_token",chave).defaultHeader("User-Agent","DietaMais").build();
		this.mapper=mapper;this.relogio=relogio;this.tokenWebhook=tokenWebhook==null?"":tokenWebhook;
	}
	@Override public boolean disponivel(){return true;}

	@Override public CheckoutCriado criarCheckout(DadosCheckout d) {
		try {
			var cliente=d.clienteId();
			if(cliente==null){
				var corpo=new LinkedHashMap<String,Object>();corpo.put("name",d.nome());corpo.put("email",d.email());corpo.put("externalReference",d.assinaturaId().toString());corpo.put("notificationDisabled",false);
				cliente=http.post().uri("/v3/customers").body(corpo).retrieve().body(JsonNode.class).get("id").asString();
			}
			var assinatura=new LinkedHashMap<String,Object>();
			assinatura.put("customer",cliente);assinatura.put("billingType","UNDEFINED");assinatura.put("value",d.valorMensal());
			assinatura.put("nextDueDate",LocalDate.now(relogio.withZone(ZoneOffset.UTC)).plusDays(1).toString());assinatura.put("cycle","MONTHLY");
			assinatura.put("description","Dieta+ "+d.planoNome());assinatura.put("externalReference",d.assinaturaId().toString());
			var id=http.post().uri("/v3/subscriptions").body(assinatura).retrieve().body(JsonNode.class).get("id").asString();
			var url=urlCobranca(id).orElseThrow(()->new ExcecaoNegocio(CodigoErro.PAGAMENTO_INDISPONIVEL,"RN-ASS-06"));
			return new CheckoutCriado(cliente,id,url);
		} catch (RestClientException|NullPointerException erro) { throw indisponivel(erro); }
	}

	@Override public Optional<String> urlCobranca(String id) {
		try {
			var pagamentos=http.get().uri("/v3/subscriptions/{id}/payments",id).retrieve().body(JsonNode.class).get("data");
			if(pagamentos==null||!pagamentos.isArray()||pagamentos.isEmpty())return Optional.empty();
			var url=pagamentos.get(0).get("invoiceUrl");
			return url==null||url.isNull()?Optional.empty():Optional.of(url.asString());
		} catch (RestClientException erro) { throw indisponivel(erro); }
	}

	@Override public void cancelar(String id) {
		try { http.delete().uri("/v3/subscriptions/{id}",id).retrieve().toBodilessEntity(); }
		catch (RestClientException erro) { LoggerFactory.getLogger(GatewayAsaas.class).warn("Não foi possível cancelar a assinatura no gateway."); }
	}

	@Override public boolean tokenValido(String token) {
		return !tokenWebhook.isEmpty()&&token!=null&&MessageDigest.isEqual(tokenWebhook.getBytes(StandardCharsets.UTF_8),token.getBytes(StandardCharsets.UTF_8));
	}

	@Override public Evento interpretar(String corpo) {
		JsonNode raiz;
		try { raiz=mapper.readTree(corpo); } catch (RuntimeException erro) { throw new IllegalArgumentException("JSON inválido."); }
		if(raiz==null||!raiz.hasNonNull("event"))throw new IllegalArgumentException("Evento sem tipo.");
		var nome=raiz.get("event").asString();
		var pagamento=raiz.get("payment");var assinatura=raiz.get("subscription");
		String idGateway=null;String vencimento=null;String idPagamento=null;
		if(pagamento!=null&&pagamento.isObject()){
			idGateway=texto(pagamento,"subscription");vencimento=texto(pagamento,"dueDate");idPagamento=texto(pagamento,"id");
		}
		if(idGateway==null&&assinatura!=null&&assinatura.isObject())idGateway=texto(assinatura,"id");
		var eventoId=raiz.hasNonNull("id")?raiz.get("id").asString():nome+":"+(idPagamento!=null?idPagamento:idGateway);
		var tipo=switch(nome){
			case "PAYMENT_CONFIRMED","PAYMENT_RECEIVED" -> TipoEvento.PAGAMENTO_CONFIRMADO;
			case "PAYMENT_OVERDUE" -> TipoEvento.PAGAMENTO_ATRASADO;
			case "PAYMENT_CREDIT_CARD_CAPTURE_REFUSED","PAYMENT_REPROVED_BY_RISK_ANALYSIS" -> TipoEvento.PAGAMENTO_RECUSADO;
			case "SUBSCRIPTION_DELETED","SUBSCRIPTION_INACTIVATED" -> TipoEvento.ASSINATURA_CANCELADA;
			default -> TipoEvento.IGNORADO;
		};
		if(eventoId.length()>120)eventoId=eventoId.substring(0,120);
		return new Evento(eventoId,tipo,idGateway,vencimento);
	}
	private static String texto(JsonNode no,String campo){var v=no.get(campo);return v==null||v.isNull()?null:v.asString();}
	private static ExcecaoNegocio indisponivel(Exception erro){
		LoggerFactory.getLogger(GatewayAsaas.class).warn("Falha ao falar com o gateway de pagamento: {}",erro.getClass().getSimpleName());
		return new ExcecaoNegocio(CodigoErro.PAGAMENTO_INDISPONIVEL,"RN-ASS-06");
	}
}
