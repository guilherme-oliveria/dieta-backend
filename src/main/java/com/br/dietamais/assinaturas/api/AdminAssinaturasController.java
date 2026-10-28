package com.br.dietamais.assinaturas.api;

import com.br.dietamais.assinaturas.aplicacao.AdministrarAssinaturas;
import com.br.dietamais.comum.api.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name="admin")
@SecurityRequirement(name="bearerAuth")
@RotaAdmin
@ApiResponses({
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="Perfil que não é ADMIN (PERFIL_INVALIDO)",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="404",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class AdminAssinaturasController {
	private final AdministrarAssinaturas admin;
	public AdminAssinaturasController(AdministrarAssinaturas admin){this.admin=admin;}

	@GetMapping("/planos")
	@Operation(operationId="adminListarPlanos",summary="Listar planos (inclusive inativos)",description="BE-30.")
	@ApiResponse(responseCode="200",description="Planos",content=@Content(mediaType="application/json",array=@ArraySchema(schema=@Schema(implementation=PlanoAdminResposta.class))))
	public ResponseEntity<List<PlanoAdminResposta>> planos(){return ok(admin.planos());}

	@PostMapping("/planos")
	@Operation(operationId="adminCriarPlano",summary="Criar plano",description="Código em maiúsculas, imutável. `motivo` obrigatório (vai para o log do admin). BE-30.")
	@ApiResponse(responseCode="201",description="Plano criado",content=@Content(mediaType="application/json",schema=@Schema(implementation=PlanoAdminResposta.class)))
	public ResponseEntity<PlanoAdminResposta> criarPlano(@Valid @RequestBody CriarPlanoRequisicao requisicao){
		return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(admin.criarPlano(requisicao));
	}

	@PutMapping("/planos/{id}")
	@Operation(operationId="adminAtualizarPlano",summary="Atualizar plano",description="Preço novo vale para novos checkouts. O plano TESTE não desativa. BE-30.")
	@ApiResponse(responseCode="200",description="Plano atualizado",content=@Content(mediaType="application/json",schema=@Schema(implementation=PlanoAdminResposta.class)))
	public ResponseEntity<PlanoAdminResposta> atualizarPlano(@PathVariable UUID id,@Valid @RequestBody AtualizarPlanoRequisicao requisicao){return ok(admin.atualizarPlano(id,requisicao));}

	@GetMapping("/assinaturas")
	@Operation(operationId="adminListarAssinaturas",summary="Listar assinaturas",description="Filtros `status` e `busca` (nome da conta). Só dados de conta e uma contagem de pacientes, nunca dado clínico (RN-LGPD-05). BE-30.")
	@ApiResponse(responseCode="200",description="Página",content=@Content(mediaType="application/json",schema=@Schema(implementation=PaginaAssinaturasAdminResposta.class)))
	public ResponseEntity<PaginaAssinaturasAdminResposta> assinaturas(@RequestParam(required=false) StatusAssinatura status,@RequestParam(required=false) String busca,
			@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho){return ok(admin.assinaturas(status,busca,pagina,tamanho));}

	@GetMapping("/assinaturas/{id}")
	@Operation(operationId="adminConsultarAssinatura",summary="Consultar assinatura",description="BE-30.")
	@ApiResponse(responseCode="200",description="Assinatura",content=@Content(mediaType="application/json",schema=@Schema(implementation=AssinaturaAdminResposta.class)))
	public ResponseEntity<AssinaturaAdminResposta> assinatura(@PathVariable UUID id){return ok(admin.assinatura(id));}

	@PatchMapping("/assinaturas/{id}")
	@Operation(operationId="adminAjustarAssinatura",summary="Ajustar assinatura manualmente",description="Plano, status e/ou fim do período. `motivo` obrigatório; antes e depois vão para o log do admin. O webhook do gateway continua valendo depois do ajuste. BE-30.")
	@ApiResponse(responseCode="200",description="Assinatura ajustada",content=@Content(mediaType="application/json",schema=@Schema(implementation=AssinaturaAdminResposta.class)))
	public ResponseEntity<AssinaturaAdminResposta> ajustar(@PathVariable UUID id,@Valid @RequestBody AjustarAssinaturaRequisicao requisicao){return ok(admin.ajustar(id,requisicao));}

	private static <T> ResponseEntity<T> ok(T corpo){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(corpo);}
}
