package com.br.dietamais.relatorios.api;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.relatorios.aplicacao.ServicoPdf;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name="relatorios")
@SecurityRequirement(name="bearerAuth")
@ApiResponses({
	@ApiResponse(responseCode="401",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="403",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class))),
	@ApiResponse(responseCode="404",description="ProblemDetail com código estável",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
})
public class PdfController {
	private final ServicoPdf pdf;
	public PdfController(ServicoPdf pdf){this.pdf=pdf;}

	@GetMapping("/dietas/{id}/pdf-lista")
	@RotaNutri
	@Operation(operationId="pdfListaSubstituicao",summary="PDF da lista de substituição publicada",description="BE-50b; RN-DIE-03. Quatro versões; MEDIA é o padrão. Link de 10 min, sem cache. Grupo sem referência é omitido com aviso no PDF. Carboidratos usa a porção média e valores provisórios por grupo.")
	@ApiResponse(responseCode="200",description="Link temporário",content=@Content(mediaType="application/json",schema=@Schema(implementation=PdfResposta.class)))
	@ApiResponse(responseCode="400",description="Tamanho desconhecido: FORMATO_INVALIDO")
	public ResponseEntity<PdfResposta> pdfLista(@PathVariable UUID id,@RequestParam(defaultValue="MEDIA") TamanhoListaPdf tamanho){return resposta(pdf.listaDaNutri(id,tamanho));}

	@GetMapping("/eu/dietas/{id}/pdf-lista")
	@Operation(operationId="pdfMinhaListaSubstituicao",summary="PDF da lista de uma dieta que recebi",description="BE-50b; RN-DIE-03. Lista do snapshot recebido, inclusive vínculo encerrado. Link de 10 min; MEDIA é o padrão, CARBOIDRATOS usa a porção média.")
	@ApiResponse(responseCode="200",description="Link temporário",content=@Content(mediaType="application/json",schema=@Schema(implementation=PdfResposta.class)))
	@ApiResponse(responseCode="400",description="Tamanho desconhecido: FORMATO_INVALIDO")
	public ResponseEntity<PdfResposta> pdfMinhaLista(@PathVariable UUID id,@RequestParam(defaultValue="MEDIA") TamanhoListaPdf tamanho){return resposta(pdf.listaDoPaciente(id,tamanho));}

	@GetMapping("/dietas/{id}/pdf")
	@RotaNutri
	@Operation(operationId="pdfDieta",summary="PDF da dieta publicada",description="Link temporário (10 min) para o PDF do snapshot publicado, com nome, CRN e logo da nutri. 404 se a dieta nunca foi publicada. BE-28.")
	@ApiResponse(responseCode="200",description="Link temporário",content=@Content(mediaType="application/json",schema=@Schema(implementation=PdfResposta.class)))
	public ResponseEntity<PdfResposta> pdfDieta(@PathVariable UUID id){return resposta(pdf.dietaDaNutri(id));}

	@GetMapping("/avaliacoes/{id}/pdf")
	@RotaNutri
	@Operation(operationId="pdfAvaliacao",summary="PDF da avaliação",description="Link temporário (10 min) para o PDF da avaliação. BE-28.")
	@ApiResponse(responseCode="200",description="Link temporário",content=@Content(mediaType="application/json",schema=@Schema(implementation=PdfResposta.class)))
	public ResponseEntity<PdfResposta> pdfAvaliacao(@PathVariable UUID id){return resposta(pdf.avaliacaoDaNutri(id));}

	@GetMapping("/eu/dietas/{id}/pdf")
	@Operation(operationId="pdfMinhaDieta",summary="PDF de uma dieta que recebi",description="RN-DIE-03; o snapshot recebido, inclusive de vínculo encerrado. BE-28.")
	@ApiResponse(responseCode="200",description="Link temporário",content=@Content(mediaType="application/json",schema=@Schema(implementation=PdfResposta.class)))
	public ResponseEntity<PdfResposta> pdfMinhaDieta(@PathVariable UUID id){return resposta(pdf.dietaDoPaciente(id));}

	@GetMapping("/eu/avaliacoes/{id}/pdf")
	@Operation(operationId="pdfMinhaAvaliacao",summary="PDF de uma avaliação compartilhada comigo",description="RN-AVA-05; só as compartilhadas. BE-28.")
	@ApiResponse(responseCode="200",description="Link temporário",content=@Content(mediaType="application/json",schema=@Schema(implementation=PdfResposta.class)))
	public ResponseEntity<PdfResposta> pdfMinhaAvaliacao(@PathVariable UUID id){return resposta(pdf.avaliacaoDoPaciente(id));}

	private static ResponseEntity<PdfResposta> resposta(PdfResposta r){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(r);}
}
