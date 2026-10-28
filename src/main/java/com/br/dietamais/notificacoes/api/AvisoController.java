package com.br.dietamais.notificacoes.api;

import com.br.dietamais.comum.api.ProblemaResposta;
import com.br.dietamais.notificacoes.aplicacao.GerenciarAvisos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/eu/avisos")
@Tag(name="notificacoes")
@SecurityRequirement(name="bearerAuth")
@Validated
@ApiResponses({
	@ApiResponse(responseCode="400",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="404",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class AvisoController {
	private final GerenciarAvisos avisos;
	public AvisoController(GerenciarAvisos avisos){this.avisos=avisos;}

	@GetMapping
	@Operation(operationId="listarAvisos",summary="Central de avisos do perfil ativo",description="RN-NOT-01: todo aviso fica aqui, mesmo com o push desligado. Mais recentes primeiro; `naoLidos` conta todos, independente do filtro. BE-34.")
	@ApiResponse(responseCode="200",description="Página de avisos",content=@Content(mediaType="application/json",schema=@Schema(implementation=AvisosResposta.class)))
	public ResponseEntity<AvisosResposta> listarAvisos(@RequestParam(defaultValue="0") @Min(0) int pagina,@RequestParam(defaultValue="20") @Min(1) @Max(100) int tamanho,@RequestParam(defaultValue="false") boolean somenteNaoLidos){
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(avisos.listar(pagina,tamanho,somenteNaoLidos));
	}

	@PostMapping("/{id}/lido")
	@Operation(operationId="marcarAvisoLido",summary="Marcar aviso como lido",description="Idempotente. Aviso de outro usuário ou perfil = 404. BE-34.")
	@ApiResponse(responseCode="200",description="Aviso",content=@Content(mediaType="application/json",schema=@Schema(implementation=AvisoResposta.class)))
	public ResponseEntity<AvisoResposta> marcarAvisoLido(@PathVariable UUID id){
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(avisos.marcarLido(id));
	}

	@PostMapping("/lidos")
	@Operation(operationId="marcarTodosAvisosLidos",summary="Marcar todos os avisos como lidos",description="Só os do perfil ativo. BE-34.")
	@ApiResponse(responseCode="204",description="Marcados")
	public ResponseEntity<Void> marcarTodosAvisosLidos(){avisos.marcarTodosLidos();return ResponseEntity.noContent().build();}
}
