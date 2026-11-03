package com.br.dietamais.contas.api;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.contas.aplicacao.ExcluirContaPaciente;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name="contas")
@SecurityRequirement(name="bearerAuth")
public class ExclusaoContaController {
	private final ExcluirContaPaciente exclusao;
	public ExclusaoContaController(ExcluirContaPaciente exclusao){this.exclusao=exclusao;}

	@PostMapping("/eu/excluir-conta")
	@Operation(operationId="excluirMinhaConta",summary="Excluir a conta do paciente",description="RN-LGPD-03. Exige a senha. Anonimiza a identidade, encerra os vínculos e revoga as sessões; o prontuário de cada nutri fica retido pelo prazo legal. Irreversível. BE-29b.")
	@ApiResponse(responseCode="204",description="Conta excluída")
	@ApiResponse(responseCode="401",description="Senha incorreta (CREDENCIAIS_INVALIDAS) ou sessão inválida",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	@ApiResponse(responseCode="403",description="Só o paciente",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	public ResponseEntity<Void> excluirMinhaConta(@Valid @RequestBody ExcluirContaRequisicao requisicao){
		exclusao.excluir(requisicao.senha());
		return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
	}
}
