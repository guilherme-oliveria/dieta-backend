package com.br.dietamais.pacientes.api;

import java.util.UUID;

import com.br.dietamais.comum.api.ProblemaResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "pacientes")
public class ConviteController {
	private final com.br.dietamais.pacientes.aplicacao.GerenciarConvites convites;
	public ConviteController(com.br.dietamais.pacientes.aplicacao.GerenciarConvites convites){this.convites=convites;}

	@GetMapping("/convites/{codigo}")
	@Operation(operationId = "consultarConvite", summary = "Consultar nutri e termo do convite",
			description = "RN-VIN-02 e RN-VIN-04. Público; não expõe nome, e-mail nem ficha do paciente. Implementação: BE-07.")
	@ApiResponse(responseCode = "200", description = "Resposta prevista após implementação",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConviteResposta.class)))
	@ApiResponse(responseCode = "422", description = "Regra de negócio violada",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "429", description = "Limite de taxa ou bloqueio; consultar Retry-After",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	public ResponseEntity<ConviteResposta> consultarConvite(@PathVariable String codigo) {
		return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(convites.consultar(codigo));
	}

	@PostMapping("/convites/{codigo}/aceitar")
	@Operation(operationId = "aceitarConvite", summary = "Aceitar convite com consentimento",
			description = "RN-VIN-03/04. Conta nova envia cadastro; conta existente envia Bearer válido e cadastro nulo. IP vem da requisição. Retorna sessão PACIENTE. Implementação: BE-07.")
	@ApiResponse(responseCode = "200", description = "Resposta prevista após implementação",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = AceiteConviteResposta.class)))
	@ApiResponse(responseCode = "400", description = "Entrada inválida",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "401", description = "Não autenticado ou credencial/token inválido",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "409", description = "Duplicidade ou conflito de versão; consultar atual",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "422", description = "Regra de negócio violada",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "429", description = "Limite de taxa ou bloqueio; consultar Retry-After",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@com.br.dietamais.comum.api.Idempotente
	public ResponseEntity<AceiteConviteResposta> aceitarConvite(@PathVariable String codigo,
			@jakarta.validation.Valid @RequestBody AceitarConviteRequisicao requisicao,jakarta.servlet.http.HttpServletRequest pedido,
			@Parameter(description = "UUID da operação; repetição em 24 h devolve a mesma resposta")
			@RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey) {
		String ip=pedido.getRemoteAddr();if(ip.contains("%"))ip=ip.substring(0,ip.indexOf('%'));
		return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(convites.aceitar(codigo,requisicao,ip));
	}
}
