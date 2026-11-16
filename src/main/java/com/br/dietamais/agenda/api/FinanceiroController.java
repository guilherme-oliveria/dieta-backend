package com.br.dietamais.agenda.api;

import com.br.dietamais.agenda.aplicacao.GerenciarFinanceiro;
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
@Tag(name="financeiro")
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
public class FinanceiroController {
	private final GerenciarFinanceiro financeiro;
	public FinanceiroController(GerenciarFinanceiro financeiro){this.financeiro=financeiro;}

	@GetMapping("/consultas/{id}/financeiro")
	@Operation(operationId="consultarFinanceiroConsulta",summary="Financeiro da consulta",description="BE-65, RN-FIN-01; só a nutri dona; sem lançamento devolve PENDENTE com `versao` 0 (use `If-Match: \"0\"` no primeiro PUT).")
	@ApiResponse(responseCode="200",description="Financeiro da consulta",content=@Content(mediaType="application/json",schema=@Schema(implementation=FinanceiroConsultaResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<FinanceiroConsultaResposta> consultarFinanceiroConsulta(@PathVariable UUID id){
		var r=financeiro.consultar(id);return entidade(r);
	}

	@PutMapping("/consultas/{id}/financeiro")
	@Operation(operationId="atualizarFinanceiroConsulta",summary="Lançar comparecimento, valor e pagamento",description="BE-65, RN-FIN-01, RN-SYN-02; substitui o financeiro; `pago` exige valor, forma e comparecimento; consulta cancelada: 422 `CONSULTA_CANCELADA`; vínculo encerrado só lê.")
	@ApiResponse(responseCode="200",description="Financeiro atualizado",content=@Content(mediaType="application/json",schema=@Schema(implementation=FinanceiroConsultaResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<FinanceiroConsultaResposta> atualizarFinanceiroConsulta(@PathVariable UUID id,@Parameter(description="Versão entre aspas, por exemplo \"0\"") @RequestHeader("If-Match") String ifMatch,@Valid @RequestBody AtualizarFinanceiroConsultaRequisicao requisicao){
		return entidade(financeiro.atualizar(id,ifMatch,requisicao));
	}

	@GetMapping("/eu/nutri/financeiro")
	@Operation(operationId="resumoFinanceiro",summary="Resumo financeiro do período",description="BE-65, RN-FIN-01; janela [de, ate) (padrão: últimos 30 dias até amanhã; máximo 366 dias); consultas canceladas não contam.")
	@ApiResponse(responseCode="200",description="Resumo do período",content=@Content(mediaType="application/json",schema=@Schema(implementation=ResumoFinanceiroResposta.class)))
	public ResponseEntity<ResumoFinanceiroResposta> resumoFinanceiro(
			@Parameter(description="Início da janela (inclusive)") @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant de,
			@Parameter(description="Fim da janela (exclusivo)") @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant ate){
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(financeiro.resumo(de,ate));
	}

	private ResponseEntity<FinanceiroConsultaResposta> entidade(FinanceiroConsultaResposta r){return ResponseEntity.ok().eTag(Long.toString(r.versao())).cacheControl(CacheControl.noStore()).body(r);}
}
