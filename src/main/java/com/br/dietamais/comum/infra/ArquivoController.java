package com.br.dietamais.comum.infra;

import io.swagger.v3.oas.annotations.Hidden;
import java.util.Optional;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Entrega de arquivos do armazenamento local por URL assinada e de validade curta. Sem sessão: a assinatura é a credencial. */
@RestController
@Hidden
class ArquivoController {
	private final com.br.dietamais.comum.api.Armazenamento armazenamentoAtual;
	ArquivoController(com.br.dietamais.comum.api.Armazenamento armazenamentoAtual){this.armazenamentoAtual=armazenamentoAtual;}

	@GetMapping("/api/v1/arquivos/{token}")
	ResponseEntity<byte[]> baixar(@PathVariable String token,@RequestParam long exp,@RequestParam(defaultValue="") String nome,
			@RequestParam(defaultValue="application/octet-stream") String tipo,@RequestParam(required=false) String sig) {
		if(!(armazenamentoAtual instanceof ArmazenamentoLocal armazenamento))return ResponseEntity.notFound().build();
		var chave=armazenamento.validar(token,exp,nome,tipo,sig);
		if(chave.isEmpty())return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		var conteudo=armazenamento.ler(chave.get());
		if(conteudo.isEmpty())return ResponseEntity.notFound().build();
		var cabecalhos=ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").contentType(MediaType.parseMediaType(tipo));
		if(!nome.isBlank())cabecalhos.header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(nome,java.nio.charset.StandardCharsets.UTF_8).build().toString());
		return cabecalhos.body(conteudo.get());
	}
}
