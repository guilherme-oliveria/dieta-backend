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
	private final com.br.dietamais.alimentos.aplicacao.GerenciarAlimentos alimentos;
	private final com.br.dietamais.alimentos.aplicacao.GerenciarListaPaciente listas;
	public AlimentoController(com.br.dietamais.alimentos.aplicacao.ConsultarGruposAlimentos grupos,com.br.dietamais.alimentos.aplicacao.GerenciarAlimentos alimentos,com.br.dietamais.alimentos.aplicacao.GerenciarListaPaciente listas){this.grupos=grupos;this.alimentos=alimentos;this.listas=listas;}
	@GetMapping("/grupos-alimentos")
	@Operation(operationId="listarGruposAlimentos",summary="Listar grupos de alimentos",description="RN-ALI-01, RN-ALI-02. Cópia própria da nutri autenticada com versão e origem; kcal do PDF e macros iniciais estimados por representantes TACO normalizados por energia. Método/fontes em docs/arquitetura/catalogo-base.md. BE-09.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=GruposAlimentosResposta.class)))
	public ResponseEntity<GruposAlimentosResposta> listarGruposAlimentos() {
		return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(grupos.executar());
	}

	@PatchMapping("/grupos-alimentos/{id}")
	@Operation(operationId="atualizarMacrosGrupo",summary="Ajustar macros da cópia própria do grupo",description="RN-ALI-01/04 e RN-SYN-02. If-Match da cópia; não altera catálogo global ou outras nutris. Implementação BE-10.")
	@ApiResponse(responseCode="200",description="Cópia própria atualizada",content=@Content(mediaType="application/json",schema=@Schema(implementation=GrupoAlimentoResposta.class)),headers=@Header(name="ETag",description="Versão da cópia própria",schema=@Schema(type="string")))
	public ResponseEntity<GrupoAlimentoResposta> atualizarMacrosGrupo(@PathVariable UUID id,@RequestHeader("If-Match") String ifMatch,@Valid @RequestBody AtualizarMacrosGrupoRequisicao requisicao) {
		var resposta=alimentos.atualizarMacros(id,ifMatch,requisicao);return entidade(resposta,resposta.versao(),200);
	}

	@GetMapping("/alimentos")
	@Operation(operationId="listarAlimentos",summary="Listar alimentos da nutri",description="RN-ALI-01; implementação BE-10. Operação própria implementada na BE-10.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=PaginaAlimentosResposta.class)))
	public ResponseEntity<PaginaAlimentosResposta> listarAlimentos(@RequestParam(defaultValue="0") @jakarta.validation.constraints.Min(0) int pagina, @RequestParam(defaultValue="20") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int tamanho, @RequestParam(required=false) UUID grupoId, @RequestParam(required=false) @jakarta.validation.constraints.Size(max=150) String busca, @RequestParam(defaultValue="true") boolean ativo, @RequestParam(required=false) java.util.List<MarcadorAlimento> semMarcador) {
		return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(alimentos.listar(pagina,tamanho,grupoId,busca,ativo,semMarcador==null?java.util.List.of():semMarcador));
	}

	@PostMapping("/alimentos")
	@Operation(operationId="criarAlimento",summary="Criar alimento",description="RN-ALI-01; implementação BE-10. Operação própria implementada na BE-10.")
	@ApiResponse(responseCode="201",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=AlimentoResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	@Idempotente
	public ResponseEntity<AlimentoResposta> criarAlimento(@RequestHeader(value="Idempotency-Key",required=false) UUID chave, @Valid @RequestBody CriarAlimentoRequisicao requisicao) {
		var resposta=alimentos.criar(requisicao);return entidade(resposta,resposta.versao(),201);
	}

	@PatchMapping("/alimentos/{id}")
	@Operation(operationId="atualizarAlimento",summary="Atualizar ou desativar alimento",description="RN-ALI-03; desativar preserva snapshots; implementação BE-10. Operação própria implementada na BE-10.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=AlimentoResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<AlimentoResposta> atualizarAlimento(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody AtualizarAlimentoRequisicao requisicao) {
		var resposta=alimentos.atualizar(id,ifMatch,requisicao);return entidade(resposta,resposta.versao(),200);
	}

	@GetMapping("/pacientes/{id}/lista-substituicao")
	@Operation(operationId="consultarListaSubstituicao",summary="Consultar lista de substituição do paciente",description="RN-ALI-01; lista efetiva e diferenças próprias; implementação BE-10. Operação própria implementada na BE-10.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=ListaSubstituicaoResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ListaSubstituicaoResposta> consultarListaSubstituicao(@PathVariable UUID id) {
		var resposta=listas.consultar(id);return entidade(resposta,resposta.versao(),200);
	}

	@PutMapping("/pacientes/{id}/lista-substituicao")
	@Operation(operationId="atualizarListaSubstituicao",summary="Atualizar diferenças da lista do paciente",description="RN-ALI-01; substitui diferenças sem alterar outra lista; implementação BE-10. Operação própria implementada na BE-10.")
	@ApiResponse(responseCode="200",description="Resposta prevista após implementação",content=@Content(mediaType="application/json",schema=@Schema(implementation=ListaSubstituicaoResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ListaSubstituicaoResposta> atualizarListaSubstituicao(@PathVariable UUID id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody AtualizarListaSubstituicaoRequisicao requisicao) {
		var resposta=listas.atualizar(id,ifMatch,requisicao);return entidade(resposta,resposta.versao(),200);
	}

	private <T> ResponseEntity<T> entidade(T resposta,long versao,int status){return ResponseEntity.status(status).eTag(Long.toString(versao)).cacheControl(org.springframework.http.CacheControl.noStore()).body(resposta);}
}
