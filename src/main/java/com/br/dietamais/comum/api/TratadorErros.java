package com.br.dietamais.comum.api;

import java.util.List;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class TratadorErros {
	private final FabricaProblema fabrica;

	public TratadorErros(FabricaProblema fabrica) { this.fabrica = fabrica; }

	@ExceptionHandler(ExcecaoNegocio.class)
	ResponseEntity<ProblemDetail> negocio(ExcecaoNegocio erro) {
		return resposta(erro.codigo(), erro.regra(), erro.getMessage(), erro.campos(), erro.atual());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> validacao(MethodArgumentNotValidException erro) {
		if(erro.getBindingResult().getFieldErrors().stream().anyMatch(campo->"aceitouTermo".equals(campo.getField())&&"AssertTrue".equals(campo.getCode())))
			return resposta(CodigoErro.CONSENTIMENTO_OBRIGATORIO,"RN-VIN-04","É necessário aceitar o consentimento.",List.of(),null);
		var campos = erro.getBindingResult().getFieldErrors().stream().map(campo -> {
			boolean obrigatorio = List.of("NotNull", "NotBlank", "NotEmpty").contains(campo.getCode());
			return new ErroCampoResposta(campo.getField(), obrigatorio ? "OBRIGATORIO" : "FORMATO_INVALIDO",
					obrigatorio ? "Informe este campo." : "Informe um valor válido.");
		}).distinct().toList();
		var codigo = campos.stream().anyMatch(campo -> campo.codigo().equals("OBRIGATORIO"))
				? CodigoErro.OBRIGATORIO : CodigoErro.FORMATO_INVALIDO;
		return resposta(codigo, null, "Revise os campos informados.", campos, null);
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, ServletRequestBindingException.class,
			MethodArgumentTypeMismatchException.class, jakarta.validation.ConstraintViolationException.class,
			org.springframework.web.method.annotation.HandlerMethodValidationException.class })
	ResponseEntity<ProblemDetail> formato(Exception erro) {
		return resposta(CodigoErro.FORMATO_INVALIDO, null, "Requisição em formato inválido.", List.of(), null);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ProblemDetail> naoEncontrado(NoResourceFoundException erro) {
		return resposta(CodigoErro.RECURSO_NAO_ENCONTRADO, null, "Recurso não encontrado.", List.of(), null);
	}

	@ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ProblemDetail> metodo(org.springframework.web.HttpRequestMethodNotSupportedException erro) {
		var problema = fabrica.criar(CodigoErro.METODO_NAO_PERMITIDO, null, "Método não permitido.", List.of(), null);
		var metodos = erro.getSupportedHttpMethods();
		var resposta = ResponseEntity.status(405);
		if (metodos != null) resposta.allow(metodos.toArray(org.springframework.http.HttpMethod[]::new));
		return resposta.body(problema);
	}

	@ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
	ResponseEntity<ProblemDetail> conteudo(org.springframework.web.HttpMediaTypeNotSupportedException erro) {
		return resposta(CodigoErro.TIPO_CONTEUDO_NAO_SUPORTADO, null, "Tipo de conteúdo não suportado.", List.of(), null);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ProblemDetail> acesso(AccessDeniedException erro) {
		return resposta(CodigoErro.ACESSO_NEGADO, null, "Acesso não permitido.", List.of(), null);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> inesperado(Exception erro) {
		// Não registrar a exceção: mensagens de bibliotecas podem conter valores sensíveis.
		LoggerFactory.getLogger(TratadorErros.class).error("Falha não tratada; traceId={}", MDC.get("traceId"));
		return resposta(CodigoErro.ERRO_INTERNO, null, "Não foi possível concluir a operação.", List.of(), null);
	}

	private ResponseEntity<ProblemDetail> resposta(CodigoErro codigo, String regra, String detalhe,
			List<ErroCampoResposta> campos, tools.jackson.databind.JsonNode atual) {
		return ResponseEntity.status(codigo.status()).body(fabrica.criar(codigo, regra, detalhe, campos, atual));
	}
}
