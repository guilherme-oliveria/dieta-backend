package com.br.dietamais.contas.api;

import jakarta.validation.Valid;
import com.br.dietamais.comum.api.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name="contas")
@SecurityRequirement(name="bearerAuth")
@RotaNutri
@ApiResponses({
	@ApiResponse(responseCode="400",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="404",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="409",description="Conflito de versão ou idempotência; atual só se autorizado",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="422",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class PerfilNutriController {
	private final com.br.dietamais.contas.aplicacao.GerenciarPerfilNutri perfil;
	public PerfilNutriController(com.br.dietamais.contas.aplicacao.GerenciarPerfilNutri perfil){this.perfil=perfil;}
	@GetMapping("/eu/nutri")
	@Operation(operationId="consultarPerfilNutri",summary="Consultar perfil da nutri",description="Perfil próprio com nome de exibição, CRN, telefone, orientação padrão e versão. BE-33.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=PerfilNutriResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<PerfilNutriResposta> consultarPerfilNutri() {
		var resposta=perfil.consultar();return ResponseEntity.ok().eTag(resposta.versao().toString()).cacheControl(org.springframework.http.CacheControl.noStore()).body(resposta);
	}

	@PatchMapping("/eu/nutri")
	@Operation(operationId="atualizarPerfilNutri",summary="Atualizar perfil da nutri",description="RN-SYN-02 e RN-ASS-04; substitui campos editáveis próprios, opcional nulo limpa. Não altera a conta global. BE-33.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=PerfilNutriResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<PerfilNutriResposta> atualizarPerfilNutri(@RequestHeader("If-Match") String ifMatch, @Valid @RequestBody AtualizarPerfilNutriRequisicao requisicao) {
		var resposta=perfil.atualizar(ifMatch,requisicao);return ResponseEntity.ok().eTag(resposta.versao().toString()).cacheControl(org.springframework.http.CacheControl.noStore()).body(resposta);
	}

}
