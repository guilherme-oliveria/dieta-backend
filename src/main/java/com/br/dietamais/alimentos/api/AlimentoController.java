package com.br.dietamais.alimentos.api;

import java.util.UUID;
import jakarta.validation.Valid;
import com.br.dietamais.comum.api.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name="alimentos")
@SecurityRequirement(name="bearerAuth")
@RotaNutri
@ApiResponses({
	@ApiResponse(responseCode="400",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="404",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="409",description="Conflito de versão ou idempotência; atual só se autorizado",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="422",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class AlimentoController {
	private final com.br.dietamais.alimentos.aplicacao.ConsultarGruposAlimentos grupos;
	public AlimentoController(com.br.dietamais.alimentos.aplicacao.ConsultarGruposAlimentos grupos){this.grupos=grupos;}
	@GetMapping("/grupos-alimentos")
	@Operation(operationId="listarGruposAlimentos",summary="Listar grupos de alimentos",description="RN-ALI-01, RN-ALI-02. Catálogo base global para nutris autenticadas; kcal do PDF e macros iniciais estimados por representantes TACO normalizados por energia. Método/fontes em docs/arquitetura/catalogo-base.md. BE-09.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=GruposAlimentosResposta.class)))
	public ResponseEntity<GruposAlimentosResposta> listarGruposAlimentos() {
		return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(grupos.executar());
	}

	@GetMapping("/alimentos")
	@ApiResponse(responseCode="501",description="Contrato publicado; implementação BE-10 pendente",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	@Operation(operationId="listarAlimentos",summary="Listar alimentos da nutri",description="RN-ALI-01; implementação BE-10. Nesta entrega responde 501, sem executar regras da fase 2.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=PaginaAlimentosResposta.class)))
	public ResponseEntity<ProblemaResposta> listarAlimentos(@RequestParam(defaultValue="0") @jakarta.validation.constraints.Min(0) int pagina, @RequestParam(defaultValue="20") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int tamanho, @RequestParam(required=false) UUID grupoId, @RequestParam(required=false) @jakarta.validation.constraints.Size(max=150) String busca, @RequestParam(defaultValue="true") boolean ativo) {
		return ContratoNaoImplementado.resposta("fase 2");
	}

	@PostMapping("/alimentos")
	@ApiResponse(responseCode="501",description="Contrato publicado; implementação BE-10 pendente",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	@Operation(operationId="criarAlimento",summary="Criar alimento",description="RN-ALI-01; implementação BE-10. Nesta entrega responde 501, sem executar regras da fase 2.")
	@ApiResponse(responseCode="201",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=AlimentoResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ProblemaResposta> criarAlimento(@RequestHeader(value="Idempotency-Key",required=false) UUID chave, @Valid @RequestBody CriarAlimentoRequisicao requisicao) {
		return ContratoNaoImplementado.resposta("fase 2");
	}

	@PatchMapping("/alimentos/{id}")
	@ApiResponse(responseCode="501",description="Contrato publicado; implementação BE-10 pendente",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	@Operation(operationId="atualizarAlimento",summary="Atualizar ou desativar alimento",description="RN-ALI-03; desativar preserva snapshots; implementação BE-10. Nesta entrega responde 501, sem executar regras da fase 2.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=AlimentoResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ProblemaResposta> atualizarAlimento(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody AtualizarAlimentoRequisicao requisicao) {
		return ContratoNaoImplementado.resposta("fase 2");
	}

	@GetMapping("/pacientes/{id}/lista-substituicao")
	@ApiResponse(responseCode="501",description="Contrato publicado; implementação BE-10 pendente",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	@Operation(operationId="consultarListaSubstituicao",summary="Consultar lista de substituição do paciente",description="RN-ALI-01; lista efetiva e diferenças próprias; implementação BE-10. Nesta entrega responde 501, sem executar regras da fase 2.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=ListaSubstituicaoResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ProblemaResposta> consultarListaSubstituicao(@PathVariable UUID id) {
		return ContratoNaoImplementado.resposta("fase 2");
	}

	@PutMapping("/pacientes/{id}/lista-substituicao")
	@ApiResponse(responseCode="501",description="Contrato publicado; implementação BE-10 pendente",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	@Operation(operationId="atualizarListaSubstituicao",summary="Atualizar diferenças da lista do paciente",description="RN-ALI-01; substitui diferenças sem alterar outra lista; implementação BE-10. Nesta entrega responde 501, sem executar regras da fase 2.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=ListaSubstituicaoResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ProblemaResposta> atualizarListaSubstituicao(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody AtualizarListaSubstituicaoRequisicao requisicao) {
		return ContratoNaoImplementado.resposta("fase 2");
	}

}
