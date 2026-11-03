package com.br.dietamais.avaliacoes.api;

import com.br.dietamais.avaliacoes.aplicacao.GerenciarAvaliacoes;
import com.br.dietamais.comum.api.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name="avaliacoes")
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
public class AvaliacaoController {
	private final GerenciarAvaliacoes avaliacoes;
	public AvaliacaoController(GerenciarAvaliacoes avaliacoes){this.avaliacoes=avaliacoes;}

	@GetMapping("/pacientes/{id}/avaliacoes")
	@Operation(operationId="listarAvaliacoesPaciente",summary="Listar avaliações do paciente",description="RN-AVA-01; mais recentes primeiro; BE-19.")
	@ApiResponse(responseCode="200",description="Avaliações da nutri para o paciente",content=@Content(mediaType="application/json",schema=@Schema(implementation=AvaliacoesResposta.class)))
	public ResponseEntity<AvaliacoesResposta> listarAvaliacoesPaciente(@PathVariable UUID id){
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(avaliacoes.listar(id));
	}

	@PostMapping("/pacientes/{id}/avaliacoes")
	@Idempotente
	@Operation(operationId="criarAvaliacao",summary="Registrar avaliação",description="RN-AVA-01/02/03/05; o IMC e a classificação vêm do backend; BE-19.")
	@ApiResponse(responseCode="201",description="Avaliação criada",content=@Content(mediaType="application/json",schema=@Schema(implementation=AvaliacaoResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<AvaliacaoResposta> criarAvaliacao(@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) UUID chave,@Valid @RequestBody SalvarAvaliacaoRequisicao requisicao){
		return entidade(avaliacoes.criar(id,requisicao),201);
	}

	@GetMapping("/avaliacoes/{id}")
	@Operation(operationId="consultarAvaliacao",summary="Consultar avaliação",description="RN-AVA-01; BE-19.")
	@ApiResponse(responseCode="200",description="Avaliação",content=@Content(mediaType="application/json",schema=@Schema(implementation=AvaliacaoResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<AvaliacaoResposta> consultarAvaliacao(@PathVariable UUID id){return entidade(avaliacoes.consultar(id),200);}

	@PutMapping("/avaliacoes/{id}")
	@Operation(operationId="atualizarAvaliacao",summary="Substituir avaliação",description="RN-AVA-04 (30 dias; depois exige justificativa), RN-SYN-02; BE-19.")
	@ApiResponse(responseCode="200",description="Avaliação atualizada",content=@Content(mediaType="application/json",schema=@Schema(implementation=AvaliacaoResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<AvaliacaoResposta> atualizarAvaliacao(@PathVariable UUID id,@RequestHeader("If-Match") String ifMatch,@Valid @RequestBody SalvarAvaliacaoRequisicao requisicao){
		return entidade(avaliacoes.atualizar(id,ifMatch,requisicao),200);
	}

	private ResponseEntity<AvaliacaoResposta> entidade(AvaliacaoResposta r,int status){return ResponseEntity.status(status).eTag(Long.toString(r.versao())).cacheControl(CacheControl.noStore()).body(r);}
}
