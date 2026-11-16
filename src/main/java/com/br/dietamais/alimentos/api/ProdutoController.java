package com.br.dietamais.alimentos.api;

import java.util.UUID;
import jakarta.validation.Valid;
import com.br.dietamais.alimentos.aplicacao.GerenciarProdutos;
import com.br.dietamais.comum.api.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Produtos e suplementos da nutri (BE-48b). O catálogo base de 400 produtos é só leitura em {@code /referencias/produtos}. */
@RestController
@RequestMapping("/api/v1/produtos")
@Tag(name="alimentos")
@SecurityRequirement(name="bearerAuth")
@RotaNutri
@ApiResponses({
	@ApiResponse(responseCode="400",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="404",description="Produto inexistente ou de outra nutri",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="409",description="Conflito de versão ou idempotência; atual só se autorizado",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="422",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class ProdutoController {
	private final GerenciarProdutos produtos;
	public ProdutoController(GerenciarProdutos produtos){this.produtos=produtos;}

	@GetMapping
	@Operation(operationId="listarProdutos",summary="Listar produtos da nutri",description="RN-ALI-05. Só os dela; `busca` são palavras do nome ou da marca, `ativo` (padrão true) separa os desativados. BE-48b.")
	@ApiResponse(responseCode="200",description="Página de produtos",content=@Content(mediaType="application/json",schema=@Schema(implementation=PaginaProdutosResposta.class)))
	public ResponseEntity<PaginaProdutosResposta> listarProdutos(@RequestParam(defaultValue="0") @jakarta.validation.constraints.Min(0) int pagina,@RequestParam(defaultValue="20") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int tamanho,
			@RequestParam(required=false) @jakarta.validation.constraints.Size(max=150) String busca,@RequestParam(required=false) @jakarta.validation.constraints.Size(max=80) String classificacao,@RequestParam(defaultValue="true") boolean ativo){
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(produtos.listar(pagina,tamanho,busca,classificacao,ativo));
	}

	@GetMapping("/{id}")
	@Operation(operationId="consultarProduto",summary="Consultar um produto da nutri",description="RN-ALI-05. De outra nutri: 404. BE-48b.")
	@ApiResponse(responseCode="200",description="Produto",content=@Content(mediaType="application/json",schema=@Schema(implementation=ProdutoResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ProdutoResposta> consultarProduto(@PathVariable UUID id){var r=produtos.consultar(id);return entidade(r,200);}

	@PostMapping
	@Operation(operationId="criarProduto",summary="Cadastrar produto ou suplemento",description="RN-ALI-05. Nutrientes por 100 g (ou 100 mL) com energia, carboidrato, proteína e lipídeos; chaves iguais às de `/referencias/produtos`. `id` opcional (UUIDv7 do app). Exige assinatura ativa. BE-48b.")
	@ApiResponse(responseCode="201",description="Produto criado",content=@Content(mediaType="application/json",schema=@Schema(implementation=ProdutoResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(implementation=String.class)))
	@Idempotente
	public ResponseEntity<ProdutoResposta> criarProduto(@RequestHeader(value="Idempotency-Key",required=false) UUID chave,@Valid @RequestBody CriarProdutoRequisicao requisicao){
		return entidade(produtos.criar(requisicao),201);
	}

	@PatchMapping("/{id}")
	@Operation(operationId="atualizarProduto",summary="Atualizar ou desativar produto",description="RN-ALI-05. `If-Match` com a versão; `ativo=false` desativa sem apagar. BE-48b.")
	@ApiResponse(responseCode="200",description="Produto atualizado",content=@Content(mediaType="application/json",schema=@Schema(implementation=ProdutoResposta.class)),headers=@Header(name="ETag",description="Versão forte da entidade para If-Match",schema=@Schema(type="string")))
	public ResponseEntity<ProdutoResposta> atualizarProduto(@PathVariable UUID id,@RequestHeader("If-Match") String ifMatch,@Valid @RequestBody AtualizarProdutoRequisicao requisicao){
		return entidade(produtos.atualizar(id,ifMatch,requisicao),200);
	}

	private ResponseEntity<ProdutoResposta> entidade(ProdutoResposta r,int status){return ResponseEntity.status(status).eTag(Long.toString(r.versao())).cacheControl(CacheControl.noStore()).body(r);}
}
