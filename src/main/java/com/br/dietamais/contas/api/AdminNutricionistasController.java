package com.br.dietamais.contas.api;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.aplicacao.AdministrarNutricionistas;
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
@RequestMapping("/api/v1/admin")
@Tag(name="admin")
@SecurityRequirement(name="bearerAuth")
@RotaAdmin
@ApiResponses({
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="Perfil que não é ADMIN (PERFIL_INVALIDO)",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="404",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class AdminNutricionistasController {
	private final AdministrarNutricionistas admin;
	public AdminNutricionistasController(AdministrarNutricionistas admin){this.admin=admin;}

	@GetMapping("/nutricionistas")
	@Operation(operationId="adminListarNutricionistas",summary="Listar nutricionistas",description="Nome, e-mail, CRN e se o CRN foi verificado; filtros `busca` e `verificado`. Nenhum dado de paciente (RN-LGPD-05). BE-30.")
	@ApiResponse(responseCode="200",description="Página",content=@Content(mediaType="application/json",schema=@Schema(implementation=PaginaNutrisAdminResposta.class)))
	public ResponseEntity<PaginaNutrisAdminResposta> listar(@RequestParam(required=false) String busca,@RequestParam(required=false) Boolean verificado,
			@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho){
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(admin.listar(busca,verificado,pagina,tamanho));
	}

	@PutMapping("/nutricionistas/{id}/crn-verificado")
	@Operation(operationId="adminVerificarCrn",summary="Marcar o CRN como verificado (ou desfazer)",description="`motivo` obrigatório; fica no log do admin. Editar o CRN pela nutri zera a verificação. BE-30.")
	@ApiResponse(responseCode="200",description="Nutricionista atualizada",content=@Content(mediaType="application/json",schema=@Schema(implementation=NutriAdminResposta.class)))
	public ResponseEntity<NutriAdminResposta> verificarCrn(@PathVariable UUID id,@Valid @RequestBody VerificarCrnRequisicao requisicao){
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(admin.verificarCrn(id,requisicao));
	}
}
