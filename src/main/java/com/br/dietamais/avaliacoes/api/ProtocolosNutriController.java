package com.br.dietamais.avaliacoes.api;

import com.br.dietamais.avaliacoes.aplicacao.GerenciarProtocolosNutri;
import com.br.dietamais.comum.api.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name="avaliacoes")
@SecurityRequirement(name="bearerAuth")
@RotaNutri
@ApiResponses({
	@ApiResponse(responseCode="400",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="409",description="Conflito de versão; atual só se autorizado",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="422",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class ProtocolosNutriController {
	private final GerenciarProtocolosNutri protocolos;
	public ProtocolosNutriController(GerenciarProtocolosNutri protocolos){this.protocolos=protocolos;}

	@GetMapping("/eu/nutri/protocolos")
	@Operation(operationId="consultarProtocolosNutri",summary="Consultar Meus protocolos",description="Padrão da nutri para as próximas avaliações; SETE/MAGRO na versão 0 se nunca salvou. BE-42.")
	@ApiResponse(responseCode="200",description="Protocolos",content=@Content(mediaType="application/json",schema=@Schema(implementation=ProtocolosNutriResposta.class)),headers=@Header(name="ETag",description="Versão forte"))
	public ResponseEntity<ProtocolosNutriResposta> consultarProtocolosNutri() {
		var r=protocolos.consultar();return ResponseEntity.ok().eTag(r.versao().toString()).cacheControl(CacheControl.noStore()).body(r);
	}

	@PutMapping("/eu/nutri/protocolos")
	@Operation(operationId="salvarProtocolosNutri",summary="Salvar Meus protocolos",description="RN-CAL-03 e RN-SYN-02. Vale para as avaliações novas; as já gravadas guardam o que usaram. Valor desconhecido: 422 PROTOCOLO_INVALIDO. BE-42.")
	@ApiResponse(responseCode="200",description="Protocolos salvos",content=@Content(mediaType="application/json",schema=@Schema(implementation=ProtocolosNutriResposta.class)),headers=@Header(name="ETag",description="Nova versão"))
	public ResponseEntity<ProtocolosNutriResposta> salvarProtocolosNutri(@RequestHeader("If-Match") String ifMatch,@Valid @RequestBody SalvarProtocolosNutriRequisicao requisicao) {
		var r=protocolos.atualizar(ifMatch,requisicao);return ResponseEntity.ok().eTag(r.versao().toString()).cacheControl(CacheControl.noStore()).body(r);
	}
}
