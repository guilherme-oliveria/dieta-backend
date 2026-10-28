package com.br.dietamais.agenda.api;

import com.br.dietamais.agenda.aplicacao.GerenciarConsultas;
import com.br.dietamais.comum.api.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name="agenda")
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
public class ConsultaController {
	private final GerenciarConsultas consultas;
	public ConsultaController(GerenciarConsultas consultas){this.consultas=consultas;}

	@GetMapping("/consultas")
	@Operation(operationId="listarConsultas",summary="Agenda da nutri",description="RN-AGE-01; janela [de, ate) em ISO-8601 (padrão: de ontem a 90 dias; máximo 366 dias), mais cedo primeiro, incluindo canceladas; `pacienteId` (id da ficha) restringe a um paciente; BE-23.")
	@ApiResponse(responseCode="200",description="Consultas da janela",content=@Content(mediaType="application/json",schema=@Schema(implementation=ConsultasResposta.class)))
	public ResponseEntity<ConsultasResposta> listarConsultas(
			@Parameter(description="Início da janela (inclusive)") @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant de,
			@Parameter(description="Fim da janela (exclusivo)") @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant ate,
			@Parameter(description="Id da ficha do paciente") @RequestParam(required=false) UUID pacienteId){
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(consultas.listar(pacienteId,de,ate));
	}

	@PostMapping("/consultas")
	@Idempotente
	@Operation(operationId="criarConsulta",summary="Agendar consulta",description="RN-AGE-01; vínculo encerrado só lê; início no passado (tolerância de 5 min) ou além de 2 anos: 400; BE-23.")
	@ApiResponse(responseCode="201",description="Consulta agendada",content=@Content(mediaType="application/json",schema=@Schema(implementation=ConsultaResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ConsultaResposta> criarConsulta(@RequestHeader(value="Idempotency-Key",required=false) UUID chave,@Valid @RequestBody CriarConsultaRequisicao requisicao){
		return entidade(consultas.criar(requisicao),201);
	}

	@PatchMapping("/consultas/{id}")
	@Operation(operationId="atualizarConsulta",summary="Remarcar ou cancelar consulta",description="RN-AGE-01, RN-SYN-02; campos ausentes ficam como estão; `status: CANCELADA` cancela (sem outros campos); consulta cancelada não muda mais (422 `CONSULTA_CANCELADA`); BE-23.")
	@ApiResponse(responseCode="200",description="Consulta atualizada",content=@Content(mediaType="application/json",schema=@Schema(implementation=ConsultaResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ConsultaResposta> atualizarConsulta(@PathVariable UUID id,@Parameter(description="Versão entre aspas, por exemplo \"1\"") @RequestHeader("If-Match") String ifMatch,@Valid @RequestBody AtualizarConsultaRequisicao requisicao){
		return entidade(consultas.atualizar(id,ifMatch,requisicao),200);
	}

	private ResponseEntity<ConsultaResposta> entidade(ConsultaResposta r,int status){return ResponseEntity.status(status).eTag(Long.toString(r.versao())).cacheControl(CacheControl.noStore()).body(r);}
}
