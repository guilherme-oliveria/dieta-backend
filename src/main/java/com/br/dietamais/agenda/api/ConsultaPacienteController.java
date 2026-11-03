package com.br.dietamais.agenda.api;

import com.br.dietamais.agenda.aplicacao.GerenciarConsultas;
import com.br.dietamais.comum.api.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/eu")
@Tag(name="agenda")
@SecurityRequirement(name="bearerAuth")
@ApiResponses({
	@ApiResponse(responseCode="400",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class ConsultaPacienteController {
	private final GerenciarConsultas consultas;
	public ConsultaPacienteController(GerenciarConsultas consultas){this.consultas=consultas;}

	@GetMapping("/consultas")
	@Operation(operationId="listarMinhasConsultas",summary="Listar minhas consultas",description="RN-AGE-01, RN-ASS-05; mesma janela de `GET /consultas` (padrão: de ontem a 90 dias); só leitura; BE-23.")
	@ApiResponse(responseCode="200",description="Consultas da janela",content=@Content(mediaType="application/json",schema=@Schema(implementation=ConsultasResposta.class)))
	public ResponseEntity<ConsultasResposta> listarMinhasConsultas(
			@Parameter(description="Início da janela (inclusive)") @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant de,
			@Parameter(description="Fim da janela (exclusivo)") @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant ate){
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(consultas.minhas(de,ate));
	}
}
