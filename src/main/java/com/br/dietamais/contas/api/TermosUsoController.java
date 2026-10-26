package com.br.dietamais.contas.api;

import com.br.dietamais.comum.api.*;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name="contas")
public class TermosUsoController {
	private final TermosUso termos;
	public TermosUsoController(TermosUso termos){this.termos=termos;}
	@GetMapping(value="/legal/terms-of-use",produces=MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId="consultarTermosUso",summary="Consultar termos de uso e disponibilidade de cadastro",description="Fonte interna versionada; rascunho não autoriza cadastro. RN-CON-09.")
	public ResponseEntity<TermosUsoResposta> consultar(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(termos.consultar());}
}
