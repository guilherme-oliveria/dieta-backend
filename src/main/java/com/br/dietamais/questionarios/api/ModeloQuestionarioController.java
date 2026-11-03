package com.br.dietamais.questionarios.api;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.questionarios.aplicacao.GerenciarModelos;
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
@RequestMapping("/api/v1/modelos-questionario")
@Tag(name="questionarios")
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
public class ModeloQuestionarioController {
	private final GerenciarModelos modelos;
	public ModeloQuestionarioController(GerenciarModelos modelos){this.modelos=modelos;}

	@GetMapping
	@Operation(operationId="listarModelosQuestionario",summary="Listar modelos de questionário",description="RN-ANA-01; a cópia do modelo padrão é criada no primeiro acesso; BE-21.")
	@ApiResponse(responseCode="200",description="Modelos da nutri",content=@Content(mediaType="application/json",schema=@Schema(implementation=ModelosResposta.class)))
	public ResponseEntity<ModelosResposta> listar(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(modelos.listar());}

	@PostMapping
	@Idempotente
	@Operation(operationId="criarModeloQuestionario",summary="Criar modelo de questionário",description="RN-ANA-01/02; BE-21.")
	@ApiResponse(responseCode="201",description="Modelo criado",content=@Content(mediaType="application/json",schema=@Schema(implementation=ModeloQuestionarioResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ModeloQuestionarioResposta> criar(@RequestHeader(value="Idempotency-Key",required=false) UUID chave,@Valid @RequestBody SalvarModeloRequisicao requisicao){
		return entidade(modelos.criar(requisicao),201);
	}

	@GetMapping("/{id}")
	@Operation(operationId="consultarModeloQuestionario",summary="Consultar modelo de questionário",description="RN-ANA-01; BE-21.")
	@ApiResponse(responseCode="200",description="Modelo",content=@Content(mediaType="application/json",schema=@Schema(implementation=ModeloQuestionarioResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ModeloQuestionarioResposta> consultar(@PathVariable UUID id){return entidade(modelos.consultar(id),200);}

	@PutMapping("/{id}")
	@Operation(operationId="atualizarModeloQuestionario",summary="Substituir modelo de questionário",description="RN-ANA-01/02, RN-ANA-04 (questionários já enviados não mudam), RN-SYN-02; BE-21.")
	@ApiResponse(responseCode="200",description="Modelo atualizado",content=@Content(mediaType="application/json",schema=@Schema(implementation=ModeloQuestionarioResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ModeloQuestionarioResposta> atualizar(@PathVariable UUID id,@RequestHeader("If-Match") String ifMatch,@Valid @RequestBody SalvarModeloRequisicao requisicao){
		return entidade(modelos.atualizar(id,ifMatch,requisicao),200);
	}

	private ResponseEntity<ModeloQuestionarioResposta> entidade(ModeloQuestionarioResposta r,int status){return ResponseEntity.status(status).eTag(Long.toString(r.versao())).cacheControl(CacheControl.noStore()).body(r);}
}
