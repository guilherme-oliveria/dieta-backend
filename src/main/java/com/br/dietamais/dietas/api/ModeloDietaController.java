package com.br.dietamais.dietas.api;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.dietas.aplicacao.GerenciarModelosDieta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
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
@RequestMapping("/api/v1")
@Tag(name="dietas")
@SecurityRequirement(name="bearerAuth")
@RotaNutri
@ApiResponses({
	@ApiResponse(responseCode="400",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="ProblemDetail com código estável (RN-MOD-01: modelo do sistema não se altera)",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="404",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="409",description="Conflito de versão ou idempotência; atual só se autorizado",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="422",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class ModeloDietaController {
	private final GerenciarModelosDieta modelos;
	public ModeloDietaController(GerenciarModelosDieta modelos){this.modelos=modelos;}

	@GetMapping("/eu/nutri/modelos-dieta")
	@Operation(operationId="listarModelosDieta",summary="Listar modelos de dieta (sistema e da nutri)",description="RN-MOD-01; os do sistema vêm primeiro e são somente leitura. BE-52.")
	@ApiResponse(responseCode="200",description="Modelos",content=@Content(mediaType="application/json",schema=@Schema(implementation=ModelosDietaResposta.class)))
	public ResponseEntity<ModelosDietaResposta> listarModelosDieta() {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(modelos.listar());
	}

	@GetMapping("/eu/nutri/modelos-dieta/{id}")
	@Operation(operationId="consultarModeloDieta",summary="Consultar modelo de dieta",description="Do sistema ou da nutri; de outra nutri é 404. BE-52.")
	@ApiResponse(responseCode="200",description="Modelo",content=@Content(mediaType="application/json",schema=@Schema(implementation=ModeloDietaResposta.class)),headers=@Header(name="ETag",description="Versão forte"))
	public ResponseEntity<ModeloDietaResposta> consultarModeloDieta(@PathVariable UUID id) {
		return entidade(modelos.consultar(id),200);
	}

	@PostMapping("/eu/nutri/modelos-dieta")
	@Operation(operationId="criarModeloDieta",summary="Criar modelo de dieta da nutri",description="Mesmas regras de conteúdo da dieta (RN-DIE-10, RN-DIE-11). BE-52.")
	@ApiResponse(responseCode="201",description="Modelo criado",content=@Content(mediaType="application/json",schema=@Schema(implementation=ModeloDietaResposta.class)),headers=@Header(name="ETag",description="Versão forte"))
	@Idempotente
	public ResponseEntity<ModeloDietaResposta> criarModeloDieta(@RequestHeader(value="Idempotency-Key",required=false) UUID chave,@Valid @RequestBody SalvarModeloDietaRequisicao requisicao) {
		return entidade(modelos.criar(requisicao),201);
	}

	@PostMapping("/eu/nutri/modelos-dieta/de-dieta/{dietaId}")
	@Operation(operationId="salvarDietaComoModelo",summary="Salvar uma dieta como modelo",description="O conteúdo vem da dieta (só dela); a energia total vira a referência de escala. BE-52.")
	@ApiResponse(responseCode="201",description="Modelo criado",content=@Content(mediaType="application/json",schema=@Schema(implementation=ModeloDietaResposta.class)),headers=@Header(name="ETag",description="Versão forte"))
	@Idempotente
	public ResponseEntity<ModeloDietaResposta> salvarDietaComoModelo(@PathVariable UUID dietaId,@RequestHeader(value="Idempotency-Key",required=false) UUID chave,@Valid @RequestBody SalvarComoModeloRequisicao requisicao) {
		return entidade(modelos.salvarComoModelo(dietaId,requisicao),201);
	}

	@PostMapping("/eu/nutri/modelos-dieta/{id}/duplicar")
	@Operation(operationId="duplicarModeloDieta",summary="Copiar modelo para a lista da nutri",description="Serve para modelos do sistema e dela. BE-52.")
	@ApiResponse(responseCode="201",description="Cópia criada",content=@Content(mediaType="application/json",schema=@Schema(implementation=ModeloDietaResposta.class)),headers=@Header(name="ETag",description="Versão forte"))
	@Idempotente
	public ResponseEntity<ModeloDietaResposta> duplicarModeloDieta(@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) UUID chave) {
		return entidade(modelos.duplicar(id),201);
	}

	@PutMapping("/eu/nutri/modelos-dieta/{id}")
	@Operation(operationId="atualizarModeloDieta",summary="Substituir modelo da nutri",description="RN-MOD-01 e RN-SYN-02: modelo do sistema é 403, de outra nutri é 404. BE-52.")
	@ApiResponse(responseCode="200",description="Modelo atualizado",content=@Content(mediaType="application/json",schema=@Schema(implementation=ModeloDietaResposta.class)),headers=@Header(name="ETag",description="Nova versão"))
	public ResponseEntity<ModeloDietaResposta> atualizarModeloDieta(@PathVariable UUID id,@RequestHeader("If-Match") String ifMatch,@Valid @RequestBody SalvarModeloDietaRequisicao requisicao) {
		return entidade(modelos.atualizar(id,ifMatch,requisicao),200);
	}

	@DeleteMapping("/eu/nutri/modelos-dieta/{id}")
	@Operation(operationId="excluirModeloDieta",summary="Apagar modelo da nutri",description="RN-MOD-01 e RN-SYN-02; dietas já criadas a partir dele não mudam. BE-52.")
	@ApiResponse(responseCode="204",description="Apagado")
	public ResponseEntity<Void> excluirModeloDieta(@PathVariable UUID id,@RequestHeader("If-Match") String ifMatch) {
		modelos.excluir(id,ifMatch);return ResponseEntity.noContent().build();
	}

	@PostMapping("/pacientes/{id}/dietas/de-modelo/{modeloId}")
	@Operation(operationId="criarDietaDeModelo",summary="Criar rascunho de dieta a partir de um modelo",description="RN-MOD-02: porções escaladas pelo alvo de energia, arredondadas para 0,5; nunca publica. Alimentos indisponíveis ou com restrição do modelo saem, com aviso (RN-MOD-03). BE-52.")
	@ApiResponse(responseCode="201",description="Rascunho criado",content=@Content(mediaType="application/json",schema=@Schema(implementation=DietaDeModeloResposta.class)),headers=@Header(name="ETag",description="Versão forte da dieta"))
	@Idempotente
	public ResponseEntity<DietaDeModeloResposta> criarDietaDeModelo(@PathVariable UUID id,@PathVariable UUID modeloId,@RequestHeader(value="Idempotency-Key",required=false) UUID chave,@Valid @RequestBody(required=false) AplicarModeloRequisicao requisicao) {
		var r=modelos.aplicar(id,modeloId,requisicao);
		return ResponseEntity.status(201).eTag(Long.toString(r.dieta().versao())).cacheControl(CacheControl.noStore()).body(r);
	}

	private ResponseEntity<ModeloDietaResposta> entidade(ModeloDietaResposta r,int status){return ResponseEntity.status(status).eTag(Long.toString(r.versao())).cacheControl(CacheControl.noStore()).body(r);}
}
