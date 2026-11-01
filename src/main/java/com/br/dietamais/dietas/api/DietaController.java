package com.br.dietamais.dietas.api;

import java.util.UUID;
import jakarta.validation.Valid;
import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.aplicacao.GerenciarDietas;
import com.br.dietamais.dietas.aplicacao.PublicarDietas;
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
	@ApiResponse(responseCode="422",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class DietaController {
	private final GerenciarDietas dietas;private final PublicarDietas publicacao;
	public DietaController(GerenciarDietas dietas,PublicarDietas publicacao){this.dietas=dietas;this.publicacao=publicacao;}
	@GetMapping("/pacientes/{id}/dietas")
	@Operation(operationId="listarDietasPaciente",summary="Listar dietas criadas pela nutri",description="RN-DIE-02; fichas da nutri autenticada; implementação BE-11. Implementada na BE-11.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=DietasResposta.class)))
	public ResponseEntity<DietasResposta> listarDietasPaciente(@PathVariable UUID id) {
		return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(dietas.listar(id));
	}

	@GetMapping("/pacientes/{id}/dietas/referencia-avaliacao")
	@Operation(operationId="consultarReferenciaAvaliacaoDieta",summary="Peso, metas e condição da avaliação mais recente",description="RN-CAL-05, BE-49d; fichas da nutri autenticada; 404 sem avaliação dela.")
	@ApiResponse(responseCode="200",description="Referência da avaliação para o planejador",content=@Content(mediaType="application/json",schema=@Schema(implementation=ReferenciaAvaliacaoDietaResposta.class)))
	public ResponseEntity<ReferenciaAvaliacaoDietaResposta> consultarReferenciaAvaliacaoDieta(@PathVariable UUID id) {
		return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(dietas.referenciaAvaliacao(id));
	}

	@PostMapping("/pacientes/{id}/dietas")
	@Operation(operationId="criarDieta",summary="Criar rascunho de dieta",description="RN-DIE-01, RN-DIE-05; totais são calculados no backend; implementação BE-11. Implementada na BE-11.")
	@ApiResponse(responseCode="201",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=DietaResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	@Idempotente
	public ResponseEntity<DietaResposta> criarDieta(@PathVariable UUID id, @RequestHeader(value="Idempotency-Key",required=false) UUID chave, @Valid @RequestBody SalvarDietaRequisicao requisicao) {
		var resposta=dietas.criar(id,requisicao);return entidade(resposta,201);
	}

	@GetMapping("/dietas/{id}")
	@Operation(operationId="consultarDieta",summary="Consultar dieta",description="RN-DIE-01; implementação BE-11. Implementada na BE-11.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=DietaResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<DietaResposta> consultarDieta(@PathVariable UUID id) {
		return entidade(dietas.consultar(id),200);
	}

	@PutMapping("/dietas/{id}")
	@Operation(operationId="atualizarDieta",summary="Salvar rascunho inteiro",description="RN-DIE-04, RN-SYN-02; substitui rascunho inteiro; implementação BE-11. Implementada na BE-11.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=DietaResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<DietaResposta> atualizarDieta(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody SalvarDietaRequisicao requisicao) {
		return entidade(dietas.atualizar(id,ifMatch,requisicao),200);
	}

	@PostMapping("/dietas/{id}/publicar")
	@Operation(operationId="publicarDieta",summary="Publicar dieta e congelar snapshot",description="RN-DIE-02, RN-DIE-03, RN-DIE-06, RN-DIE-08, RN-DIE-09; implementada na BE-12.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=DietaSnapshotResposta.class)))
	@Idempotente
	public ResponseEntity<DietaSnapshotResposta> publicarDieta(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch, @RequestHeader(value="Idempotency-Key",required=false) UUID chave) {
		return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(publicacao.publicar(id,ifMatch));
	}

	@PostMapping("/dietas/{id}/duplicar")
	@Operation(operationId="duplicarDieta",summary="Duplicar dieta como rascunho",description="RN-DIE-04; novo rascunho; implementada na BE-12.")
	@ApiResponse(responseCode="201",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=DietaResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	@Idempotente
	public ResponseEntity<DietaResposta> duplicarDieta(@PathVariable UUID id, @RequestHeader(value="Idempotency-Key",required=false) UUID chave) {
		return entidade(publicacao.duplicar(id),201);
	}

	private ResponseEntity<DietaResposta> entidade(DietaResposta resposta,int status){return ResponseEntity.status(status).eTag(Long.toString(resposta.versao())).cacheControl(org.springframework.http.CacheControl.noStore()).body(resposta);}
}
