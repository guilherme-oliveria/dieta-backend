package com.br.dietamais.avaliacoes.api;

import com.br.dietamais.avaliacoes.aplicacao.GerenciarAvaliacoes;
import com.br.dietamais.comum.api.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/eu")
@Tag(name="avaliacoes")
@SecurityRequirement(name="bearerAuth")
@ApiResponses({
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class AvaliacaoPacienteController {
	private final GerenciarAvaliacoes avaliacoes;
	public AvaliacaoPacienteController(GerenciarAvaliacoes avaliacoes){this.avaliacoes=avaliacoes;}

	@GetMapping("/avaliacoes")
	@Operation(operationId="listarMinhasAvaliacoes",summary="Listar avaliações compartilhadas comigo",description="RN-AVA-05, RN-ASS-05; só as marcadas como compartilhadas; BE-19.")
	@ApiResponse(responseCode="200",description="Avaliações compartilhadas",content=@Content(mediaType="application/json",schema=@Schema(implementation=AvaliacoesResposta.class)))
	public ResponseEntity<AvaliacoesResposta> listarMinhasAvaliacoes(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(avaliacoes.minhas());}
}
