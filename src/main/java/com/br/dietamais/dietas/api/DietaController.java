package com.br.dietamais.dietas.api;

import java.util.UUID;
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
@Tag(name="dietas")
@SecurityRequirement(name="bearerAuth")
@RotaNutri
@ApiResponses({
	@ApiResponse(responseCode="400",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="404",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="409",description="Conflito de versão ou idempotência; atual só se autorizado",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="422",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="501",description="Contrato publicado; implementação pendente",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class DietaController {
	@GetMapping("/pacientes/{id}/dietas")
	@Operation(operationId="listarDietasPaciente",summary="Listar dietas criadas pela nutri",description="RN-DIE-02; fichas da nutri autenticada; implementação BE-11. Nesta entrega responde 501, sem executar regras da fase 2.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=DietasResposta.class)))
	public ResponseEntity<ProblemaResposta> listarDietasPaciente(@PathVariable UUID id) {
		return ContratoNaoImplementado.resposta("fase 2");
	}

	@PostMapping("/pacientes/{id}/dietas")
	@Operation(operationId="criarDieta",summary="Criar rascunho de dieta",description="RN-DIE-01, RN-DIE-05; totais são calculados no backend; implementação BE-11. Nesta entrega responde 501, sem executar regras da fase 2.")
	@ApiResponse(responseCode="201",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=DietaResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ProblemaResposta> criarDieta(@PathVariable UUID id, @RequestHeader(value="Idempotency-Key",required=false) UUID chave, @Valid @RequestBody SalvarDietaRequisicao requisicao) {
		return ContratoNaoImplementado.resposta("fase 2");
	}

	@GetMapping("/dietas/{id}")
	@Operation(operationId="consultarDieta",summary="Consultar dieta",description="RN-DIE-01; implementação BE-11. Nesta entrega responde 501, sem executar regras da fase 2.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=DietaResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ProblemaResposta> consultarDieta(@PathVariable UUID id) {
		return ContratoNaoImplementado.resposta("fase 2");
	}

	@PutMapping("/dietas/{id}")
	@Operation(operationId="atualizarDieta",summary="Salvar rascunho inteiro",description="RN-DIE-04, RN-SYN-02; substitui rascunho inteiro; implementação BE-11. Nesta entrega responde 501, sem executar regras da fase 2.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=DietaResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ProblemaResposta> atualizarDieta(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody SalvarDietaRequisicao requisicao) {
		return ContratoNaoImplementado.resposta("fase 2");
	}

	@PostMapping("/dietas/{id}/publicar")
	@Operation(operationId="publicarDieta",summary="Publicar dieta e congelar snapshot",description="RN-DIE-02, RN-DIE-03, RN-DIE-06, RN-DIE-08, RN-DIE-09; implementação BE-12. Nesta entrega responde 501, sem executar regras da fase 2.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=DietaSnapshotResposta.class)))
	public ResponseEntity<ProblemaResposta> publicarDieta(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch, @RequestHeader(value="Idempotency-Key",required=false) UUID chave) {
		return ContratoNaoImplementado.resposta("fase 2");
	}

	@PostMapping("/dietas/{id}/duplicar")
	@Operation(operationId="duplicarDieta",summary="Duplicar dieta como rascunho",description="RN-DIE-04; novo rascunho; implementação BE-12. Nesta entrega responde 501, sem executar regras da fase 2.")
	@ApiResponse(responseCode="201",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=DietaResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ProblemaResposta> duplicarDieta(@PathVariable UUID id, @RequestHeader(value="Idempotency-Key",required=false) UUID chave) {
		return ContratoNaoImplementado.resposta("fase 2");
	}

}
