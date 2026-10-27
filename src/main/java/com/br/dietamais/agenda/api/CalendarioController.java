package com.br.dietamais.agenda.api;

import com.br.dietamais.agenda.aplicacao.GerenciarCalendario;
import com.br.dietamais.comum.api.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name="agenda")
public class CalendarioController {
	private final GerenciarCalendario calendario;
	public CalendarioController(GerenciarCalendario calendario){this.calendario=calendario;}

	@PostMapping("/api/v1/eu/calendario/token")
	@Operation(operationId="gerarTokenCalendario",summary="Gera o link secreto do feed .ics (revoga o anterior)",description="Perfil PACIENTE. O token só aparece nesta resposta. Implementação: BE-17.")
	@SecurityRequirement(name="bearerAuth")
	@ApiResponse(responseCode="201",description="Token e links",content=@Content(mediaType="application/json",schema=@Schema(implementation=TokenCalendarioResposta.class)))
	@ApiResponse(responseCode="401",description="Não autenticado",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	@ApiResponse(responseCode="403",description="Perfil negado",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	public ResponseEntity<TokenCalendarioResposta> gerar(){
		return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(calendario.gerarToken());
	}

	@DeleteMapping("/api/v1/eu/calendario/token")
	@Operation(operationId="revogarTokenCalendario",summary="Revoga o link do feed .ics",description="Perfil PACIENTE. Idempotente. Implementação: BE-17.")
	@SecurityRequirement(name="bearerAuth")
	@ApiResponse(responseCode="204",description="Revogado (ou já não existia)")
	@ApiResponse(responseCode="401",description="Não autenticado",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	@ApiResponse(responseCode="403",description="Perfil negado",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	public ResponseEntity<Void> revogar(){calendario.revogar();return ResponseEntity.noContent().build();}

	@GetMapping(value="/cal/{arquivo}",produces="text/calendar;charset=UTF-8")
	@Operation(operationId="baixarCalendario",summary="Feed .ics do paciente (público; o token é a credencial)",description="Não usa Bearer. Token inexistente ou revogado: 404. Implementação: BE-17.")
	@ApiResponse(responseCode="200",description="iCalendar",content=@Content(mediaType="text/calendar",schema=@Schema(type="string")))
	@ApiResponse(responseCode="404",description="Token inexistente ou revogado",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ProblemaResposta.class)))
	public ResponseEntity<String> baixar(@PathVariable String arquivo){
		if(!arquivo.endsWith(".ics"))throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		var token=arquivo.substring(0,arquivo.length()-4);
		if(!token.matches("[A-Za-z0-9_-]{43}"))throw new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null);
		var feed=calendario.feed(token).orElseThrow(()->new ExcecaoNegocio(CodigoErro.RECURSO_NAO_ENCONTRADO,null));
		return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/calendar;charset=UTF-8")).cacheControl(CacheControl.noCache().cachePrivate()).body(feed);
	}
}
