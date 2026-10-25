package com.br.dietamais.pacientes.api;

import java.util.UUID;

import com.br.dietamais.comum.api.ContratoNaoImplementado;
import com.br.dietamais.comum.api.ProblemaResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "pacientes")
@com.br.dietamais.comum.api.RotaNutri
public class PacienteController {
	private final com.br.dietamais.pacientes.aplicacao.GerenciarPacientes pacientes;
	public PacienteController(com.br.dietamais.pacientes.aplicacao.GerenciarPacientes pacientes){this.pacientes=pacientes;}

	@GetMapping("/pacientes")
	@Operation(operationId = "listarPacientes", summary = "Listar pacientes vinculados à nutri",
			description = "RN-VIN-01. Busca por nome e filtro de status; adesao7d nula até BE-16. Implementação: BE-06.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponse(responseCode = "200", description = "Resposta prevista após implementação",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginaPacientesResposta.class)))
	@ApiResponse(responseCode = "400", description = "Entrada inválida",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "401", description = "Não autenticado ou credencial/token inválido",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "403", description = "Acesso ou perfil negado",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	public ResponseEntity<PaginaPacientesResposta> listarPacientes(@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho,
			@RequestParam(defaultValue = "nome,asc") String ordem, @RequestParam(required = false) String busca,
			@RequestParam(required = false) StatusVinculo status) {
		return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(pacientes.listar(pagina,tamanho,ordem,busca,status));
	}

	@PostMapping("/pacientes")
	@Operation(operationId = "criarPaciente", summary = "Criar ficha de paciente e vínculo CONVIDADO",
			description = "RN-ASS-03, RN-ASS-04 e RN-VIN-07. id pode ser UUIDv7 do cliente; dono vem do usuário autenticado. Implementação: BE-06.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponse(responseCode = "201", description = "Resposta prevista após implementação",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = PacienteResposta.class)))
	@ApiResponse(responseCode = "400", description = "Entrada inválida",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "401", description = "Não autenticado ou credencial/token inválido",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "403", description = "Acesso ou perfil negado",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "409", description = "Duplicidade ou conflito de versão; consultar atual",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "422", description = "Regra de negócio violada",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@com.br.dietamais.comum.api.Idempotente
	public ResponseEntity<PacienteResposta> criarPaciente(@jakarta.validation.Valid @RequestBody CriarPacienteRequisicao requisicao,
			@Parameter(description = "UUID da operação; repetição em 24 h devolve a mesma resposta")
			@RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey) {
		var paciente=pacientes.criar(requisicao);
		return ResponseEntity.created(java.net.URI.create("/api/v1/pacientes/"+paciente.id()))
			.eTag(paciente.versao().toString()).cacheControl(org.springframework.http.CacheControl.noStore()).body(paciente);
	}

	@GetMapping("/pacientes/{id}")
	@Operation(operationId = "consultarPaciente", summary = "Consultar ficha de paciente",
			description = "RN-VIN-01. ETag contém a versão. Recurso inexistente ou de outra nutri: 404. Implementação: BE-06.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponse(responseCode = "200", description = "Resposta prevista após implementação",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = PacienteResposta.class)),
			headers = @Header(name = "ETag", description = "Versão entre aspas", schema = @Schema(type = "string")))
	@ApiResponse(responseCode = "401", description = "Não autenticado ou credencial/token inválido",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "403", description = "Acesso ou perfil negado",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "404", description = "Recurso inexistente ou fora da visibilidade",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	public ResponseEntity<PacienteResposta> consultarPaciente(@PathVariable UUID id) {
		var paciente=pacientes.consultar(id);
		return ResponseEntity.ok().eTag(paciente.versao().toString()).cacheControl(org.springframework.http.CacheControl.noStore()).body(paciente);
	}

	@PatchMapping("/pacientes/{id}")
	@Operation(operationId = "atualizarPaciente", summary = "Atualizar dados da ficha",
			description = "RN-SYN-02. Envia o conjunto de campos editáveis da ficha; opcional nulo limpa o campo. If-Match obrigatório; conflito traz atual. Implementação: BE-06.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponse(responseCode = "200", description = "Resposta prevista após implementação",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = PacienteResposta.class)),
			headers = @Header(name = "ETag", description = "Versão entre aspas", schema = @Schema(type = "string")))
	@ApiResponse(responseCode = "400", description = "Entrada inválida",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "401", description = "Não autenticado ou credencial/token inválido",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "403", description = "Acesso ou perfil negado",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "404", description = "Recurso inexistente ou fora da visibilidade",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "409", description = "Duplicidade ou conflito de versão; consultar atual",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "422", description = "Regra de negócio violada",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	public ResponseEntity<PacienteResposta> atualizarPaciente(@PathVariable UUID id,
			@Parameter(description = "Versão entre aspas, por exemplo \"1\"") @RequestHeader("If-Match") String ifMatch,
			@jakarta.validation.Valid @RequestBody AtualizarPacienteRequisicao requisicao) {
		var paciente=pacientes.atualizar(id,ifMatch,requisicao);
		return ResponseEntity.ok().eTag(paciente.versao().toString()).cacheControl(org.springframework.http.CacheControl.noStore()).body(paciente);
	}

	@PostMapping("/pacientes/{id}/convite")
	@ApiResponse(responseCode = "501", description = "Contrato publicado; implementação pendente",
		content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@Operation(operationId = "convidarPaciente", summary = "Gerar ou reenviar convite",
			description = "RN-CON-05, RN-ASS-03 e RN-VIN-02. Não recebe corpo; novo convite invalida o anterior. Implementação: BE-07.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponse(responseCode = "201", description = "Resposta prevista após implementação",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConviteCriadoResposta.class)))
	@ApiResponse(responseCode = "401", description = "Não autenticado ou credencial/token inválido",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "403", description = "Acesso ou perfil negado",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "404", description = "Recurso inexistente ou fora da visibilidade",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "409", description = "Duplicidade ou conflito de versão; consultar atual",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "422", description = "Regra de negócio violada",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@com.br.dietamais.comum.api.Idempotente
	public ResponseEntity<ProblemaResposta> convidarPaciente(@PathVariable UUID id,
			@Parameter(description = "UUID da operação; repetição em 24 h devolve a mesma resposta")
			@RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey) {
		return ContratoNaoImplementado.resposta();
	}

	@PostMapping("/pacientes/{id}/encerrar-vinculo")
	@ApiResponse(responseCode = "501", description = "Contrato publicado; implementação pendente",
		content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@com.br.dietamais.comum.api.RotaNutri(exclusiva=false)
	@Operation(operationId = "encerrarVinculo", summary = "Encerrar vínculo",
			description = "RN-VIN-05. Sem corpo; nutri dona ou o próprio paciente pode encerrar. Mantém prontuário só leitura e dietas recebidas. Implementação: BE-07.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponse(responseCode = "200", description = "Resposta prevista após implementação",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = VinculoResposta.class)))
	@ApiResponse(responseCode = "401", description = "Não autenticado ou credencial/token inválido",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "403", description = "Acesso ou perfil negado",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "404", description = "Recurso inexistente ou fora da visibilidade",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@ApiResponse(responseCode = "422", description = "Regra de negócio violada",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	@com.br.dietamais.comum.api.Idempotente
	@ApiResponse(responseCode = "409", description = "Chave de idempotência reutilizada para outra requisição",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemaResposta.class)))
	public ResponseEntity<ProblemaResposta> encerrarVinculo(@PathVariable UUID id,
			@Parameter(description = "UUID da operação; repetição em 24 h devolve a mesma resposta")
			@RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey) {
		return ContratoNaoImplementado.resposta();
	}
}
