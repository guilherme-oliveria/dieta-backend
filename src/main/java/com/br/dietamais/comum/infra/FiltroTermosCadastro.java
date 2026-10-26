package com.br.dietamais.comum.infra;

import com.br.dietamais.comum.api.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@Order(-94) // Confere a publicação antes de um eventual replay idempotente (-90).
public class FiltroTermosCadastro extends OncePerRequestFilter {
	private final TermosUso termos;private final FabricaProblema problemas;private final JsonMapper mapper;
	public FiltroTermosCadastro(TermosUso termos,FabricaProblema problemas,JsonMapper mapper){this.termos=termos;this.problemas=problemas;this.mapper=mapper;}
	@Override protected boolean shouldNotFilter(HttpServletRequest pedido) {
		return !"POST".equals(pedido.getMethod())||!("/api/v1/auth/cadastro-nutri".equals(pedido.getRequestURI())||pedido.getRequestURI().matches("/api/v1/convites/[^/]+/aceitar"));
	}
	@Override protected void doFilterInternal(HttpServletRequest pedido,HttpServletResponse resposta,FilterChain cadeia) throws IOException,ServletException {
		byte[] corpo=pedido.getInputStream().readNBytes(1048577);
		if(corpo.length>1048576){problemas.escrever(resposta,CodigoErro.FORMATO_INVALIDO,"Requisição excede o limite permitido.");return;}
		JsonNode entrada;
		try {entrada=mapper.readTree(corpo);}
		catch(RuntimeException erro){problemas.escrever(resposta,CodigoErro.FORMATO_INVALIDO,"Requisição em formato inválido.");return;}
		if(entrada==null||!entrada.isObject()){problemas.escrever(resposta,CodigoErro.FORMATO_INVALIDO,"Requisição em formato inválido.");return;}
		JsonNode cadastro="/api/v1/auth/cadastro-nutri".equals(pedido.getRequestURI())?entrada:entrada.path("cadastro");
		if(!cadastro.isMissingNode()&&!cadastro.isNull()) {
			var aceite=cadastro.path("aceitouTermos");var versao=cadastro.path("versaoTermos");
			try {termos.validar(aceite.isBoolean()?aceite.asBoolean():null,versao.isString()?versao.asString().strip():null);}
			catch(ExcecaoNegocio erro){problemas.escrever(resposta,erro);return;}
		}
		cadeia.doFilter(new RequisicaoRepetivel(pedido,corpo),resposta);
	}
}
