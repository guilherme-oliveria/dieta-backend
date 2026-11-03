package com.br.dietamais.notificacoes.api;

public record MensagemEmail(String destinatario, String assunto, String html) {
	@Override public String toString() { return "MensagemEmail[conteúdo protegido]"; }
}
