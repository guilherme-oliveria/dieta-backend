package com.br.dietamais.contas.api;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.aplicacao.GerenciarDispositivos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/eu")
@Tag(name="contas")
@SecurityRequirement(name="bearerAuth")
@ApiResponses({
	@ApiResponse(responseCode="400",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="404",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="409",description="Conflito de versão; atual no corpo",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class DispositivoController {
	private final GerenciarDispositivos dispositivos;
	public DispositivoController(GerenciarDispositivos dispositivos){this.dispositivos=dispositivos;}

	@PostMapping("/dispositivos")
	@Operation(operationId="registrarDispositivo",summary="Registra o token de push do aparelho",description="Idempotente por token; se o token era de outra conta, passa para esta. Desktop não usa. Máximo de 10 aparelhos por conta (os mais antigos saem). Implementação: BE-18.")
	@ApiResponse(responseCode="201",description="Registrado",content=@Content(mediaType="application/json",schema=@Schema(implementation=DispositivoResposta.class)))
	public ResponseEntity<DispositivoResposta> registrar(@Valid @RequestBody RegistrarDispositivoRequisicao requisicao){
		return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(dispositivos.registrar(requisicao));
	}

	@DeleteMapping("/dispositivos/{id}")
	@Operation(operationId="removerDispositivo",summary="Remove o aparelho (ex.: no logout)",description="Só aparelhos da própria conta; outro dono = 404. Implementação: BE-18.")
	@ApiResponse(responseCode="204",description="Removido")
	public ResponseEntity<Void> remover(@PathVariable UUID id){dispositivos.remover(id);return ResponseEntity.noContent().build();}

	@GetMapping("/preferencias-notificacao")
	@Operation(operationId="consultarPreferenciasNotificacao",summary="Preferências de push do perfil ativo",description="RN-NOT-03. Padrão: tudo ligado, silêncio 22:00 a 07:00, versão 0. Implementação: BE-18.")
	@ApiResponse(responseCode="200",description="Preferências",content=@Content(mediaType="application/json",schema=@Schema(implementation=PreferenciasNotificacaoResposta.class)),headers=@Header(name="ETag",description="Versão forte"))
	public ResponseEntity<PreferenciasNotificacaoResposta> consultar(){
		var r=dispositivos.consultar();return ResponseEntity.ok().eTag(r.versao().toString()).cacheControl(CacheControl.noStore()).body(r);
	}

	@PutMapping("/preferencias-notificacao")
	@Operation(operationId="atualizarPreferenciasNotificacao",summary="Atualiza as preferências de push",description="RN-NOT-03 e RN-SYN-02. Exige If-Match. Tipos ausentes ficam ligados; tipo de outro perfil ou repetido = 400. Implementação: BE-18.")
	@ApiResponse(responseCode="200",description="Preferências",content=@Content(mediaType="application/json",schema=@Schema(implementation=PreferenciasNotificacaoResposta.class)),headers=@Header(name="ETag",description="Versão forte"))
	public ResponseEntity<PreferenciasNotificacaoResposta> atualizar(@RequestHeader("If-Match") String ifMatch,@Valid @RequestBody AtualizarPreferenciasRequisicao requisicao){
		var r=dispositivos.atualizar(ifMatch,requisicao);return ResponseEntity.ok().eTag(r.versao().toString()).cacheControl(CacheControl.noStore()).body(r);
	}
}
