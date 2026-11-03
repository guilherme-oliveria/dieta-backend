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
@ApiResponses({
	@ApiResponse(responseCode="400",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="404",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="409",description="Conflito de versão ou idempotência; atual só se autorizado",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="422",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
})
public class DietaPacienteController {
	private final com.br.dietamais.dietas.aplicacao.ConsultarDietasPaciente dietas;
	public DietaPacienteController(com.br.dietamais.dietas.aplicacao.ConsultarDietasPaciente dietas){this.dietas=dietas;}
	@GetMapping("/eu/dietas")
	@Operation(operationId="listarMinhasDietas",summary="Listar snapshots recebidos pelo paciente",description="RN-DIE-03, RN-DIE-07, RN-ASS-05; snapshots recebidos inclusive histórico; implementada na BE-13.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=DietasPacienteResposta.class)),headers=@Header(name="ETag",description="Versão da seleção de dieta em uso",schema=@Schema(type="string")))
	public ResponseEntity<DietasPacienteResposta> listarMinhasDietas() {
		var resposta=dietas.listar();return ResponseEntity.ok().eTag(Long.toString(resposta.versao())).cacheControl(org.springframework.http.CacheControl.noStore()).body(resposta);
	}

	@PutMapping("/eu/dieta-em-uso")
	@Operation(operationId="selecionarDietaEmUso",summary="Selecionar dieta em uso",description="RN-DIE-07; dieta do próprio paciente; implementada na BE-13.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=DietaEmUsoResposta.class)),headers=@Header(name="ETag",description="Versão da seleção de dieta em uso",schema=@Schema(type="string")))
	public ResponseEntity<DietaEmUsoResposta> selecionarDietaEmUso(@RequestHeader("If-Match") String ifMatch, @Valid @RequestBody SelecionarDietaEmUsoRequisicao requisicao) {
		var resposta=dietas.selecionar(ifMatch,requisicao.dietaId());return ResponseEntity.ok().eTag(Long.toString(resposta.versao())).cacheControl(org.springframework.http.CacheControl.noStore()).body(resposta);
	}

}
