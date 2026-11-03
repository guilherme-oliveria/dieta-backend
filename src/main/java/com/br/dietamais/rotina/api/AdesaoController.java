package com.br.dietamais.rotina.api;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.rotina.aplicacao.CalcularAdesao;
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
@Tag(name="rotina")
@RotaNutri
@SecurityRequirement(name="bearerAuth")
public class AdesaoController {
	private final CalcularAdesao adesao;
	public AdesaoController(CalcularAdesao adesao){this.adesao=adesao;}

	@GetMapping("/pacientes/{id}/adesao")
	@Operation(operationId="consultarAdesao",summary="Adesão do paciente nos últimos 7 e 30 dias",description="RN-ROT-02. % de refeições FEITO ou TROCADO sobre as esperadas. Implementação: BE-16.")
	@ApiResponse(responseCode="200",description="Adesão",content=@Content(mediaType="application/json",schema=@Schema(implementation=AdesaoResposta.class)))
	@ApiResponse(responseCode="401",description="Não autenticado",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	@ApiResponse(responseCode="403",description="Perfil negado",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	@ApiResponse(responseCode="404",description="Paciente não encontrado para a nutri",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	public ResponseEntity<AdesaoResposta> consultar(@PathVariable UUID id){
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(adesao.paciente(id));
	}
}
