package com.br.dietamais.assinaturas.api;

import com.br.dietamais.assinaturas.aplicacao.ServicoWebhookPagamento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name="assinaturas")
public class WebhookPagamentoController {
	private final ServicoWebhookPagamento servico;
	public WebhookPagamentoController(ServicoWebhookPagamento servico){this.servico=servico;}

	@PostMapping("/pagamento")
	@Operation(operationId="receberWebhookPagamento",summary="Webhook do gateway de pagamento",description="Público, mas só aceita o token do gateway (cabeçalho `asaas-access-token`): sem ele, 401. Idempotente por `evento_id`; evento repetido ou desconhecido responde 200. RN-ASS-07. BE-26.")
	@ApiResponse(responseCode="200",description="Recebido")
	public ResponseEntity<Void> receberWebhookPagamento(@RequestHeader(name="asaas-access-token",required=false) String token,@RequestBody String corpo){
		servico.processar(token,corpo);
		return ResponseEntity.ok().build();
	}
}
