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
@RequestMapping("/api/v1/eu/questionarios")
@Tag(name="questionarios")
@SecurityRequirement(name="bearerAuth")
@ApiResponses({
	@ApiResponse(responseCode="400",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="404",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="409",description="Questionário já enviado (QUESTIONARIO_FECHADO)",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class QuestionarioPacienteController {
	private final GerenciarQuestionarios questionarios;
	public QuestionarioPacienteController(GerenciarQuestionarios questionarios){this.questionarios=questionarios;}

	@GetMapping
	@Operation(operationId="listarMeusQuestionarios",summary="Questionários que recebi, com as minhas respostas",description="RN-ANA-03; BE-22.")
	@ApiResponse(responseCode="200",description="Meus questionários",content=@Content(mediaType="application/json",schema=@Schema(implementation=QuestionariosResposta.class)))
	public ResponseEntity<QuestionariosResposta> meus(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(questionarios.meus());}

	@PutMapping("/{id}/respostas")
	@Operation(operationId="salvarRespostasQuestionario",summary="Salvar respostas parciais",description="RN-ANA-02/03: mescla por pergunta (valor nulo apaga); aceita salvar parcial até enviar; BE-22.")
	@ApiResponse(responseCode="200",description="Questionário com as respostas atuais",content=@Content(mediaType="application/json",schema=@Schema(implementation=QuestionarioResposta.class)),headers=@Header(name="ETag",description="Versão da entidade",schema=@Schema(type="string")))
	public ResponseEntity<QuestionarioResposta> salvarRespostas(@PathVariable UUID id,@Valid @RequestBody SalvarRespostasRequisicao requisicao){
		var q=questionarios.salvarRespostas(id,requisicao);return ResponseEntity.ok().eTag(Long.toString(q.versao())).cacheControl(CacheControl.noStore()).body(q);
	}

	@PostMapping("/{id}/enviar")
	@Idempotente
	@Operation(operationId="enviarRespostasQuestionario",summary="Concluir e enviar o questionário",description="RN-ANA-03: confere as obrigatórias visíveis e torna as respostas imutáveis; a nutri é notificada; BE-22.")
	@ApiResponse(responseCode="200",description="Questionário respondido",content=@Content(mediaType="application/json",schema=@Schema(implementation=QuestionarioResposta.class)),headers=@Header(name="ETag",description="Versão da entidade",schema=@Schema(type="string")))
	@ApiResponse(responseCode="400",description="OBRIGATORIO com a lista de perguntas sem resposta",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	public ResponseEntity<QuestionarioResposta> concluir(@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) UUID chave){
		var q=questionarios.concluir(id);return ResponseEntity.ok().eTag(Long.toString(q.versao())).cacheControl(CacheControl.noStore()).body(q);
	}
}
