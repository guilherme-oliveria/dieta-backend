package com.br.dietamais.questionarios.api;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.questionarios.aplicacao.GerenciarQuestionarios;
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
@RequestMapping("/api/v1/pacientes/{id}/questionarios")
@Tag(name="questionarios")
@SecurityRequirement(name="bearerAuth")
@RotaNutri
@ApiResponses({
	@ApiResponse(responseCode="400",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="404",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="409",description="Conflito de idempotência",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="422",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class QuestionarioController {
	private final GerenciarQuestionarios questionarios;
	public QuestionarioController(GerenciarQuestionarios questionarios){this.questionarios=questionarios;}

	@GetMapping
	@Operation(operationId="listarQuestionariosDoPaciente",summary="Questionários enviados ao paciente, com as respostas",description="RN-ANA-03; mais recentes primeiro; BE-22.")
	@ApiResponse(responseCode="200",description="Questionários do paciente",content=@Content(mediaType="application/json",schema=@Schema(implementation=QuestionariosResposta.class)))
	public ResponseEntity<QuestionariosResposta> listar(@PathVariable UUID id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(questionarios.listarDoPaciente(id));}

	@PostMapping
	@Idempotente
	@Operation(operationId="enviarQuestionario",summary="Enviar questionário ao paciente",description="RN-ANA-03/04: copia as perguntas do modelo no envio; exige vínculo ativo e assinatura em dia; BE-22.")
	@ApiResponse(responseCode="201",description="Questionário enviado",content=@Content(mediaType="application/json",schema=@Schema(implementation=QuestionarioResposta.class)),headers=@Header(name="ETag",description="Versão da entidade",schema=@Schema(type="string")))
	public ResponseEntity<QuestionarioResposta> enviar(@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) UUID chave,@Valid @RequestBody EnviarQuestionarioRequisicao requisicao){
		var q=questionarios.enviar(id,requisicao);return ResponseEntity.status(201).eTag(Long.toString(q.versao())).cacheControl(CacheControl.noStore()).body(q);
	}
}
