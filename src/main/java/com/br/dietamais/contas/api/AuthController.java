package com.br.dietamais.contas.api;

import com.br.dietamais.comum.api.SessaoResposta;
import com.br.dietamais.contas.aplicacao.CadastrarNutri;
import jakarta.validation.Valid;

import java.util.UUID;

import com.br.dietamais.comum.api.ContratoNaoImplementado;
import com.br.dietamais.comum.api.ProblemaResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "contas")
public class AuthController {
	private final CadastrarNutri cadastro;

	public AuthController(CadastrarNutri cadastro) { this.cadastro = cadastro; }

	@PostMapping("/auth/cadastro-nutri")
	@Operation(operationId = "cadastrarNutri", summary = "Cadastrar nutricionista",
			description = "RN-CON-01, RN-CON-02 e RN-CON-04. Cria conta, organização INDIVIDUAL e assinatura TESTE. Implementação: BE-02.")
	@ApiResponse(responseCode = "201", description = "Conta, organização individual e assinatura de teste criadas",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = CadastroNutriResposta.class)))
	@ApiResponse(responseCode = "400", description = "Entrada inválida",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "409", description = "Duplicidade ou conflito de versão; consultar atual",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "422", description = "Regra de negócio violada",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "500", description = "Falha interna ou configuração indisponível",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@com.br.dietamais.comum.api.Idempotente
	public ResponseEntity<CadastroNutriResposta> cadastrarNutri(@Valid @RequestBody CadastroNutriRequisicao requisicao,
			@Parameter(description = "UUID da operação; repetição em 24 h devolve a mesma resposta")
			@RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey) {
		return ResponseEntity.status(201).body(cadastro.executar(requisicao));
	}

	@PostMapping("/auth/login")
	@ApiResponse(responseCode = "501", description = "Contrato publicado; implementação pendente",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@Operation(operationId = "entrar", summary = "Entrar e escolher perfil",
			description = "RN-CON-03. perfil é opcional. Com múltiplos perfis e sem escolha, tokens e expiraEm são nulos; usuario.perfis permite repetir login com perfil. Implementação: BE-03/BE-05.")
	@ApiResponse(responseCode = "200", description = "Resposta prevista após implementação",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = SessaoResposta.class)))
	@ApiResponse(responseCode = "400", description = "Entrada inválida",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "401", description = "Não autenticado ou credencial/token inválido",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "403", description = "Acesso ou perfil negado",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "429", description = "Limite de taxa ou bloqueio; consultar Retry-After",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	public ResponseEntity<ProblemaResposta> entrar(@RequestBody LoginRequisicao requisicao) {
		return ContratoNaoImplementado.resposta();
	}

	@PostMapping("/auth/refresh")
	@ApiResponse(responseCode = "501", description = "Contrato publicado; implementação pendente",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@Operation(operationId = "renovarSessao", summary = "Renovar sessão",
			description = "Rotaciona o refresh token; reuso revoga a família. Implementação: BE-03.")
	@ApiResponse(responseCode = "200", description = "Resposta prevista após implementação",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = SessaoResposta.class)))
	@ApiResponse(responseCode = "400", description = "Entrada inválida",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "401", description = "Não autenticado ou credencial/token inválido",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	public ResponseEntity<ProblemaResposta> renovarSessao(@RequestBody RefreshRequisicao requisicao) {
		return ContratoNaoImplementado.resposta();
	}

	@PostMapping("/auth/logout")
	@ApiResponse(responseCode = "501", description = "Contrato publicado; implementação pendente",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@Operation(operationId = "sair", summary = "Encerrar sessão do dispositivo",
			description = "Revoga o refresh token informado. Implementação: BE-03.")
	@ApiResponse(responseCode = "204", description = "Resposta prevista após implementação",
			content = @Content())
	@ApiResponse(responseCode = "400", description = "Entrada inválida",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	public ResponseEntity<ProblemaResposta> sair(@RequestBody LogoutRequisicao requisicao) {
		return ContratoNaoImplementado.resposta();
	}

	@PostMapping("/auth/confirmar-email")
	@ApiResponse(responseCode = "501", description = "Contrato publicado; implementação pendente",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@Operation(operationId = "confirmarEmail", summary = "Confirmar e-mail",
			description = "RN-CON-05. Token de uso único válido por 24 h. Implementação: BE-04.")
	@ApiResponse(responseCode = "204", description = "Resposta prevista após implementação",
			content = @Content())
	@ApiResponse(responseCode = "400", description = "Entrada inválida",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "401", description = "Não autenticado ou credencial/token inválido",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	public ResponseEntity<ProblemaResposta> confirmarEmail(@RequestBody ConfirmarEmailRequisicao requisicao) {
		return ContratoNaoImplementado.resposta();
	}

	@PostMapping("/auth/esqueci-senha")
	@ApiResponse(responseCode = "501", description = "Contrato publicado; implementação pendente",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@Operation(operationId = "recuperarSenha", summary = "Solicitar recuperação de senha",
			description = "RN-CON-07. Sem corpo na resposta; não revela se o e-mail existe. Implementação: BE-04.")
	@ApiResponse(responseCode = "202", description = "Resposta prevista após implementação",
			content = @Content())
	@ApiResponse(responseCode = "400", description = "Entrada inválida",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "429", description = "Limite de taxa ou bloqueio; consultar Retry-After",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	public ResponseEntity<ProblemaResposta> recuperarSenha(@RequestBody EsqueciSenhaRequisicao requisicao) {
		return ContratoNaoImplementado.resposta();
	}

	@PostMapping("/auth/redefinir-senha")
	@ApiResponse(responseCode = "501", description = "Contrato publicado; implementação pendente",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@Operation(operationId = "redefinirSenha", summary = "Redefinir senha",
			description = "RN-CON-02 e RN-CON-07. Token de 1 h e uso único; revoga todas as sessões. Implementação: BE-04.")
	@ApiResponse(responseCode = "204", description = "Resposta prevista após implementação",
			content = @Content())
	@ApiResponse(responseCode = "400", description = "Entrada inválida",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "401", description = "Não autenticado ou credencial/token inválido",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	public ResponseEntity<ProblemaResposta> redefinirSenha(@RequestBody RedefinirSenhaRequisicao requisicao) {
		return ContratoNaoImplementado.resposta();
	}
}
