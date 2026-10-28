package com.br.dietamais.assinaturas.api;

import com.br.dietamais.assinaturas.aplicacao.ServicoAssinatura;
import com.br.dietamais.comum.api.ProblemaResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assinatura")
@Tag(name="assinaturas")
@SecurityRequirement(name="bearerAuth")
@Validated
@ApiResponses({
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="Perfil que não é nutricionista (PERFIL_INVALIDO)",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class AssinaturaController {
	private final ServicoAssinatura servico;
	public AssinaturaController(ServicoAssinatura servico){this.servico=servico;}

	@GetMapping
	@Operation(operationId="consultarAssinatura",summary="Assinatura da nutri",description="Plano, status, uso x limite, fim do período e carência (RN-ASS-04). Só leitura: funciona com a assinatura inativa. BE-26.")
	@ApiResponse(responseCode="200",description="Detalhe da assinatura",content=@Content(mediaType="application/json",schema=@Schema(implementation=AssinaturaDetalheResposta.class)))
	public ResponseEntity<AssinaturaDetalheResposta> consultarAssinatura(){
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servico.detalhe());
	}

	@PostMapping("/checkout")
	@Operation(operationId="criarCheckout",summary="Link do checkout hospedado",description="RN-ASS-06: o app abre a URL no navegador externo. `ASSINATURA_JA_ATIVA` (409) se já paga; `PAGAMENTO_INDISPONIVEL` (503) se o gateway não responde. O status só muda pelo webhook (RN-ASS-07). BE-26.")
	@ApiResponse(responseCode="200",description="URL do checkout",content=@Content(mediaType="application/json",schema=@Schema(implementation=CheckoutResposta.class)))
	@ApiResponse(responseCode="409",description="ASSINATURA_JA_ATIVA",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	@ApiResponse(responseCode="503",description="PAGAMENTO_INDISPONIVEL",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	public ResponseEntity<CheckoutResposta> criarCheckout(@Valid @RequestBody CheckoutRequisicao requisicao){
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servico.checkout(requisicao.plano()));
	}
}
