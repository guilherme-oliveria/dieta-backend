package com.br.dietamais.assinaturas.aplicacao;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Porta do gateway de pagamento (RN-ASS-06, RN-ASS-07). A implementação concreta é a Asaas {@code [SUPOSIÇÃO S3]}. */
public interface GatewayPagamento {
	record DadosCheckout(UUID assinaturaId,String nome,String email,String planoCodigo,String planoNome,BigDecimal valorMensal,String clienteId) { }
	record CheckoutCriado(String clienteId,String assinaturaGatewayId,String url) { }
	enum TipoEvento { PAGAMENTO_CONFIRMADO,PAGAMENTO_ATRASADO,PAGAMENTO_RECUSADO,ASSINATURA_CANCELADA,IGNORADO }
	/** {@code vencimento} em ISO (yyyy-MM-dd) quando o evento traz a data do vencimento da cobrança. */
	record Evento(String eventoId,TipoEvento tipo,String assinaturaGatewayId,String vencimento) { }

	boolean disponivel();
	CheckoutCriado criarCheckout(DadosCheckout dados);
	/** Link da cobrança em aberto de uma assinatura já criada no gateway. */
	Optional<String> urlCobranca(String assinaturaGatewayId);
	void cancelar(String assinaturaGatewayId);
	boolean tokenValido(String token);
	/** Traduz o corpo bruto do webhook; lança {@link IllegalArgumentException} se não for do formato do gateway. */
	Evento interpretar(String corpo);
}
