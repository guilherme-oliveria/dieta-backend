package com.br.dietamais.assinaturas.infra;

import com.br.dietamais.assinaturas.aplicacao.GatewayPagamento;
import com.br.dietamais.comum.api.*;
import java.util.Optional;

/** Sem {@code ASAAS_API_KEY} não há checkout; o webhook também é recusado. */
public class GatewayDesligado implements GatewayPagamento {
	@Override public boolean disponivel(){return false;}
	@Override public CheckoutCriado criarCheckout(DadosCheckout dados){throw new ExcecaoNegocio(CodigoErro.PAGAMENTO_INDISPONIVEL,"RN-ASS-06");}
	@Override public Optional<String> urlCobranca(String id){return Optional.empty();}
	@Override public void cancelar(String id){ }
	@Override public boolean tokenValido(String token){return false;}
	@Override public Evento interpretar(String corpo){throw new IllegalArgumentException("Gateway desligado.");}
}
