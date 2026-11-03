package com.br.dietamais.contas.api;


import com.br.dietamais.comum.api.ProblemaResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "contas")
public class EuController {
	private final com.br.dietamais.contas.aplicacao.ConsultarEu consulta;
	public EuController(com.br.dietamais.contas.aplicacao.ConsultarEu consulta){this.consulta=consulta;}

	@GetMapping("/eu")
	@Operation(operationId = "consultarEu", summary = "Consultar conta e perfis",
			description = "Lista perfis disponíveis e perfil ativo, assinatura da nutri e vínculos do paciente. Implementação: BE-05.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponse(responseCode = "200", description = "Conta atual, assinatura da nutri ou vínculos do paciente",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = EuResposta.class)))
	@ApiResponse(responseCode = "401", description = "Não autenticado ou credencial/token inválido",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "403", description = "Acesso ou perfil negado",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	public ResponseEntity<EuResposta> consultarEu() {
		return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(consulta.executar());
	}
}
