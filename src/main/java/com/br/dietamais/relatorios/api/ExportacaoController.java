package com.br.dietamais.relatorios.api;

import com.br.dietamais.comum.api.*;
import com.br.dietamais.relatorios.aplicacao.ServicoExportacao;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name="relatorios")
public class ExportacaoController {
	private final ServicoExportacao exportacao;
	public ExportacaoController(ServicoExportacao exportacao){this.exportacao=exportacao;}

	@PostMapping("/eu/exportacao")
	@SecurityRequirement(name="bearerAuth")
	@Operation(operationId="pedirExportacao",summary="Pedir a exportação dos meus dados",description="RN-LGPD-02. Gera em segundo plano (ZIP com JSON e PDFs) e avisa por e-mail com o link. Um pedido por hora; com outro em andamento: 409 EXPORTACAO_EM_ANDAMENTO. BE-29.")
	@ApiResponse(responseCode="202",description="Pedido aceito",content=@Content(mediaType="application/json",schema=@Schema(implementation=ExportacaoResposta.class)))
	@ApiResponse(responseCode="409",description="Exportação em andamento ou pedida há menos de uma hora",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	@ApiResponse(responseCode="403",description="Só o paciente",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	public ResponseEntity<ExportacaoResposta> pedirExportacao(){
		return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(exportacao.pedir());
	}

	@GetMapping("/eu/exportacao")
	@SecurityRequirement(name="bearerAuth")
	@Operation(operationId="consultarExportacao",summary="Estado da minha última exportação",description="Com status PRONTA devolve a url de download (vale 15 min; consulte de novo para renovar). 404 se nunca pediu. BE-29.")
	@ApiResponse(responseCode="200",description="Estado do pedido",content=@Content(mediaType="application/json",schema=@Schema(implementation=ExportacaoResposta.class)))
	@ApiResponse(responseCode="404",description="Nenhum pedido",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	public ResponseEntity<ExportacaoResposta> consultarExportacao(){
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(exportacao.ultima());
	}

	/** Link do e-mail: o token aleatório é a credencial; redireciona para o arquivo assinado de validade curta. */
	@GetMapping("/exportacoes/{token}")
	@Hidden
	public ResponseEntity<Void> baixar(@PathVariable String token){
		return exportacao.urlPorToken(token).map(url->ResponseEntity.status(302).cacheControl(CacheControl.noStore()).location(URI.create(url)).<Void>build())
			.orElseGet(()->ResponseEntity.notFound().build());
	}
}
