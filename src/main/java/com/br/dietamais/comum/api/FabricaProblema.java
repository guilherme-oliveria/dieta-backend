package com.br.dietamais.comum.api;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class FabricaProblema {
	private final JsonMapper mapper;

	public FabricaProblema(JsonMapper mapper) { this.mapper = mapper; }

	public ProblemDetail criar(CodigoErro codigo, String regra, String detalhe,
			List<ErroCampoResposta> campos, JsonNode atual) {
		var problema = ProblemDetail.forStatusAndDetail(org.springframework.http.HttpStatusCode.valueOf(codigo.status()), detalhe);
		String nome = codigo.name().toLowerCase(Locale.ROOT);
		problema.setType(URI.create("https://dietamais.com.br/erros/" + nome.replace('_', '-')));
		problema.setTitle(switch (codigo.status()) {
			case 400 -> "Requisição inválida";
			case 401 -> "Autenticação necessária";
			case 403 -> "Acesso não permitido";
			case 404 -> "Recurso não encontrado";
			case 405 -> "Método não permitido";
			case 409 -> "Conflito na requisição";
			case 415 -> "Tipo de conteúdo não suportado";
			case 422 -> "Regra de negócio não atendida";
			case 429 -> "Limite de tentativas atingido";
			case 501 -> "Endpoint ainda não implementado";
			default -> "Erro interno";
		});
		problema.setProperty("codigo", codigo.name());
		problema.setProperty("regra", regra);
		problema.setProperty("campos", campos);
		problema.setProperty("traceId", MDC.get("traceId"));
		problema.setProperty("atual", atual);
		return problema;
	}

	public void escrever(HttpServletResponse resposta, CodigoErro codigo, String detalhe) throws IOException {
		resposta.setStatus(codigo.status());
		resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		var problema = criar(codigo, null, detalhe, List.of(), null);
		mapper.writeValue(resposta.getOutputStream(), new ProblemaResposta(problema.getType(), problema.getTitle(),
				codigo.status(), detalhe, codigo, null, List.of(), MDC.get("traceId"), null));
	}
}
