package com.br.dietamais.avaliacoes.api;

import com.br.dietamais.avaliacoes.aplicacao.ConsultarEvolucao;
import com.br.dietamais.comum.api.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name="avaliacoes")
@SecurityRequirement(name="bearerAuth")
@ApiResponses({
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class EvolucaoController {
	private final ConsultarEvolucao evolucao;
	public EvolucaoController(ConsultarEvolucao evolucao){this.evolucao=evolucao;}

	@GetMapping("/pacientes/{id}/evolucao")
	@RotaNutri
	@Operation(operationId="consultarEvolucaoDoPaciente",summary="Séries de evolução do paciente",description="Peso, IMC, composição, perímetros (avaliações) e peso em casa (só com vínculo ativo); BE-20.")
	@ApiResponse(responseCode="200",description="Séries cronológicas",content=@Content(mediaType="application/json",schema=@Schema(implementation=EvolucaoResposta.class)))
	@ApiResponse(responseCode="404",description="Paciente não encontrado para a nutri",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	public ResponseEntity<EvolucaoResposta> doPaciente(@PathVariable UUID id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(evolucao.daNutri(id));}

	@GetMapping("/eu/evolucao")
	@Operation(operationId="consultarMinhaEvolucao",summary="Minha evolução",description="Só as avaliações compartilhadas (RN-AVA-05) e os pesos que eu registrei; BE-20.")
	@ApiResponse(responseCode="200",description="Séries cronológicas",content=@Content(mediaType="application/json",schema=@Schema(implementation=EvolucaoResposta.class)))
	public ResponseEntity<EvolucaoResposta> minha(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(evolucao.minha());}
}
