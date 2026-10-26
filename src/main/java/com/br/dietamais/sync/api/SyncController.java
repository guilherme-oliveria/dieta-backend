package com.br.dietamais.sync.api;


import com.br.dietamais.comum.api.ProblemaResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "sync")
public class SyncController {
	private final com.br.dietamais.sync.aplicacao.ServicoSyncLeitura sync;
	public SyncController(com.br.dietamais.sync.aplicacao.ServicoSyncLeitura sync){this.sync=sync;}

	@GetMapping("/sync")
	@Operation(operationId = "puxarMudancas", summary = "Puxar mudanças da fase 1",
			description = "RN-SYN-01. cursor ausente inicia carga paginada; dados filtrados por vínculo/dono. Remoção de visibilidade devolve excluido=true e dados=null. Implementação: BE-08.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponse(responseCode = "200", description = "Resposta prevista após implementação",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = SyncResposta.class)))
	@ApiResponse(responseCode = "400", description = "Entrada inválida",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "401", description = "Não autenticado ou credencial/token inválido",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "403", description = "Acesso ou perfil negado",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	public ResponseEntity<SyncResposta> puxarMudancas(@RequestParam(required = false) String cursor, @RequestParam(defaultValue = "500") int limite) {
		return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(sync.executar(cursor,limite));
	}
}
