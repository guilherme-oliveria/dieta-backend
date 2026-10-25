package com.br.dietamais.comum.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class ContratoNaoImplementado {

	private ContratoNaoImplementado() {
	}

	public static ResponseEntity<ProblemaResposta> resposta() {
		return ResponseEntity.status(501).contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(new ProblemaResposta(URI.create("https://dietamais.com.br/erros/nao-implementado"),
						"Endpoint ainda não implementado", 501,
						"Contrato publicado; implementação prevista nas tarefas da fase 1.",
						CodigoErro.NAO_IMPLEMENTADO, null, List.of(), org.slf4j.MDC.get("traceId"), null));
	}
}
