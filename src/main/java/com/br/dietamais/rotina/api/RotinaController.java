package com.br.dietamais.rotina.api;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.rotina.aplicacao.RegistrarRotina;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/eu")
@Tag(name="rotina")
@SecurityRequirement(name="bearerAuth")
@ApiResponses({
	@ApiResponse(responseCode="400",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="404",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="409",description="Id já usado por outro paciente",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="422",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class RotinaController {
	private final RegistrarRotina rotina;
	public RotinaController(RegistrarRotina rotina){this.rotina=rotina;}

	@PostMapping("/checkins")
	@Idempotente
	@Operation(operationId="registrarCheckin",summary="Registrar check-in da refeição",description="RN-ROT-01, RN-SUB-01/03; só inclusão: repetir o id devolve o mesmo registro. Até 3 dias para trás. BE-15.")
	@ApiResponse(responseCode="201",description="Check-in gravado",content=@Content(mediaType="application/json",schema=@Schema(implementation=CheckinResposta.class)))
	public ResponseEntity<CheckinResposta> registrarCheckin(@RequestHeader(value="Idempotency-Key",required=false) UUID chave,@Valid @RequestBody CheckinRequisicao requisicao){
		return criado(rotina.checkin(requisicao));
	}

	@PostMapping("/agua")
	@Idempotente
	@Operation(operationId="registrarAgua",summary="Registrar água",description="RN-ROT-03; só inclusão, 1 a 5000 ml. BE-15.")
	@ApiResponse(responseCode="201",description="Registro gravado",content=@Content(mediaType="application/json",schema=@Schema(implementation=AguaResposta.class)))
	public ResponseEntity<AguaResposta> registrarAgua(@RequestHeader(value="Idempotency-Key",required=false) UUID chave,@Valid @RequestBody AguaRequisicao requisicao){
		return criado(rotina.agua(requisicao));
	}

	@PostMapping("/pesos")
	@Idempotente
	@Operation(operationId="registrarPeso",summary="Registrar peso em casa",description="RN-ROT-03, RN-AVA-03; só inclusão, 20 a 400 kg. BE-15.")
	@ApiResponse(responseCode="201",description="Registro gravado",content=@Content(mediaType="application/json",schema=@Schema(implementation=PesoResposta.class)))
	public ResponseEntity<PesoResposta> registrarPeso(@RequestHeader(value="Idempotency-Key",required=false) UUID chave,@Valid @RequestBody PesoRequisicao requisicao){
		return criado(rotina.peso(requisicao));
	}

	private <T> ResponseEntity<T> criado(T corpo){return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(corpo);}
}
