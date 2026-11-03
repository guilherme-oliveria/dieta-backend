package com.br.dietamais.notificacoes.api;

/** Mensagem genérica de push (RN-NOT-02); o app faz sync e navega pela {@code rota}. */
public record MensagemPush(String token,String tipo,String titulo,String corpo,String rota) {
	@Override public String toString(){return "MensagemPush["+tipo+"]";}
}
